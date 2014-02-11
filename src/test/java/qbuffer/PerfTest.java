/*
 * Copyright (c) 2013 Mark D. Horton
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABIL-
 * ITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package qbuffer;

import net.nostromo.qbuffer.QBuffer;
import net.nostromo.qbuffer.QBufferConsumer;
import net.nostromo.qbuffer.QBufferProducer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

public class PerfTest {

    private final long operations;
    private final int capacity;
    private final int batchSize;

    final Map<String, long[]> results = new TreeMap<>();

    public PerfTest(final long operations, final int capacity, final int batchSize) {
        this.operations = operations;
        this.capacity = capacity;
        this.batchSize = batchSize;

        System.out.format("ops: %,d - batch: %,d%n", operations, batchSize);
    }

    public static void main(final String[] args) throws Exception {
        final boolean runSingle = true;
        final boolean runMulti = false;
        final boolean runUnit = false;
        final int warmupRuns = 0;

        final long operations = 100_000_000L;
        final int iterations = 5;
        final int arraySize = 10;

        final int[] baseBatchSizes = { 1, 10, 100, 1_000 };
        final int[] batchMultipliers = { 1 };
        final int[] queueCounts = { 2, 3 };

        final File file = new File("graph/qbuffer_tmp.dat");
        final PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));

        // warmup
        if (warmupRuns > 0) System.out.println("warmup");

        for (int x = 0; x < warmupRuns; x++) {
            final int batchSize = 1_000;
            final int capacity = batchSize * 1_000;

            final PerfTest test = new PerfTest(1_000_000_000, capacity, batchSize);

            if (runSingle) test.qbufferTest(arraySize);
            if (runMulti) test.qbufferMultipleTest(arraySize, 2);
            if (runUnit) test.qbufferUnitTest();
        }

        System.out.println("starting perf runs");

        for (int baseBatchSize : baseBatchSizes) {
            for (int batchMultiplier : batchMultipliers) {
                final int batchSize = baseBatchSize * batchMultiplier;
                final int capacity = batchSize * 1_000;

                final PerfTest test = new PerfTest(operations, capacity, batchSize);

                for (int iteration = 0; iteration < iterations; iteration++) {
                    if (runSingle) test.qbufferTest(arraySize);
                    if (runMulti) {
                        for (int queueCount : queueCounts) {
                            test.qbufferMultipleTest(arraySize, queueCount);
                        }
                    }
                    if (runUnit) test.qbufferUnitTest();
                }

                test.summarize(writer);
            }
        }

        final PerfTest test = new PerfTest(operations, 10_000, 0);
        for (int iteration = 0; iteration < iterations; iteration++) {
            test.jdkQueueTest(arraySize);
        }
        test.summarize(writer);

        writer.close();
    }

    private void summarize(final PrintWriter writer) {
        writer.printf("%d ", batchSize);

        for (final String name : results.keySet()) {
            final long[] vals = results.get(name);
            printStats("SUMMARY", vals[0], vals[1]);
            writer.printf("%d %d ", vals[0], vals[1]);
        }

        writer.println();
        writer.flush();
    }

    private void stats(final String name, final long operations, final long nanos) {
        printStats(name, operations, nanos);

        long[] vals = results.get(name);
        if (vals == null) {
            vals = new long[2];
            vals[0] = 0;
            vals[1] = 0;
            results.put(name, vals);
        }

        vals[0] += operations;
        vals[1] += nanos;
    }

    private void printStats(final String name, final long operations, final long nanos) {
        System.out.format("%15s -> ops/sec: %,11.0f - avg: %5.2f ns%n", name,
                (operations / (double) nanos) * 1_000_000_000, (double) nanos / operations);
    }

    private void jdkQueueTest(final int arraySize) throws Exception {
        final Queue<String[]> queue = new ArrayBlockingQueue<>(capacity);

        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(2);
        final String object = "hey";

        new Thread(new Runnable() {
            private long cnt;

            @Override
            public void run() {
                try {
                    startGate.await();
                    while (cnt < operations) {
                        process();
                    }
                    endGate.countDown();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            private void process() {
                final String[] arr = queue.poll();
                if (arr == null) {
                    Thread.yield();
                    return;
                }

                for (int z = 0; z < arr.length; z++) {
                    cnt++;
                }
            }
        }).start();

        new Thread(new Runnable() {
            private long cnt;

            @Override
            public void run() {
                try {
                    startGate.await();
                    while (cnt < operations) {
                        process();
                    }
                    endGate.countDown();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            private void process() {
                final String[] arr = new String[arraySize];
                for (int z = 0; z < arraySize; z++) {
                    arr[z] = object;
                }

                if (!queue.offer(arr)) {
                    Thread.yield();
                    return;
                }

                cnt += arraySize;
            }
        }).start();

        final long start = System.nanoTime();
        startGate.countDown();
        endGate.await();
        final long stop = System.nanoTime();

        stats("jdkqueue", operations, stop - start);
    }

    @SuppressWarnings("unchecked")
    private void jdkQueueMultipleTest(final int queueCount) throws Exception {
        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(queueCount + 1);
        final String object = "hey";

        final Queue<String>[] queues = new Queue[queueCount];
        final long cnts[] = new long[queueCount];

        for (int n = 0; n < queueCount; n++) {
            final Queue<String> queue = new ArrayBlockingQueue<>(capacity);
            queues[n] = queue;
            cnts[n] = 0;

            new Thread(new Runnable() {
                private long cnt;

                @Override
                public void run() {
                    try {
                        startGate.await();
                        while (cnt < operations) {
                            process();
                        }
                        endGate.countDown();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                private void process() {
                    if (queue.poll() == null) {
                        Thread.yield();
                        return;
                    }

                    cnt++;
                }
            }).start();
        }

        new Thread(new Runnable() {
            private long loops;
            private int completed;

            @Override
            public void run() {
                try {
                    startGate.await();
                    while (completed < queueCount) {
                        process(loops++);
                    }
                    endGate.countDown();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            private void process(final long x) {
                final int idx = (int) (x % queueCount);
                long cnt = cnts[idx];

                if (cnt == -1) return;

                if (cnt >= operations) {
                    cnts[idx] = -1;
                    completed++;
                    return;
                }

                final Queue<String> queue = queues[idx];

                if (!queue.offer(object)) {
                    Thread.yield();
                    return;
                }

                cnt++;
                cnts[idx] = cnt;
            }
        }).start();

        final long start = System.nanoTime();
        startGate.countDown();
        endGate.await();
        final long stop = System.nanoTime();

        stats("jdkqueue-" + queueCount, operations * queueCount, stop - start);
    }

    private void qbufferTest(final int arraySize) throws Exception {
        final QBuffer<String[]> queue = new QBuffer<>(capacity, batchSize);

        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(2);
        final String object = "hey";

        new Thread(new Runnable() {
            private final QBufferConsumer<String[]> consumer = queue.consumer();
            private long cnt;

            @Override
            public void run() {
                try {
                    startGate.await();
                    while (cnt < operations) {
                        process();
                    }
                    endGate.countDown();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            private void process() {
                final long s = consumer.begin();
                if (s == 0) {
                    Thread.yield();
                    return;
                }

                for (int y = 0; y < s; y++) {
                    final String[] arr = consumer.consume();
                    for (int z = 0; z < arr.length; z++) {
                        cnt++;
                    }
                }

                consumer.lazyMixCommit();
            }
        }).start();

        new Thread(new Runnable() {
            private final QBufferProducer<String[]> producer = queue.producer();
            private long cnt;

            @Override
            public void run() {
                try {
                    startGate.await();
                    while (cnt < operations) {
                        process();
                    }
                    endGate.countDown();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            private void process() {
                final long s = producer.begin();
                if (s == 0) {
                    Thread.yield();
                    return;
                }

                for (int y = 0; y < s; y++) {
                    final String[] arr = new String[arraySize];
                    for (int z = 0; z < arraySize; z++) {
                        arr[z] = object;
                    }

                    producer.produce(arr);
                }

                cnt += (producer.lazyMixCommit() * arraySize);
            }
        }).start();

        final long start = System.nanoTime();
        startGate.countDown();
        endGate.await();
        final long stop = System.nanoTime();

        stats("qbuffer", operations, stop - start);
    }

    @SuppressWarnings("unchecked")
    private void qbufferMultipleTest(final int arraySize, final int queueCount) throws Exception {
        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(queueCount + 1);
        final String object = "hey";

        final QBufferProducer<String[]>[] producers = new QBufferProducer[queueCount];
        final long cnts[] = new long[queueCount];

        for (int n = 0; n < queueCount; n++) {
            final QBuffer<String[]> queue = new QBuffer<>(capacity, batchSize);
            final QBufferConsumer<String[]> consumer = queue.consumer();
            producers[n] = queue.producer();
            cnts[n] = 0;

            new Thread(new Runnable() {
                private long cnt;

                @Override
                public void run() {
                    try {
                        startGate.await();
                        while (cnt < operations) {
                            process();
                        }
                        endGate.countDown();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                private void process() {
                    final long s = consumer.begin();
                    if (s == 0) {
                        Thread.yield();
                        return;
                    }

                    for (int y = 0; y < s; y++) {
                        final String[] arr = consumer.consume();
                        for (int z = 0; z < arr.length; z++) {
                            cnt++;
                        }
                    }

                    consumer.lazyMixCommit();
                }
            }).start();
        }

        new Thread(new Runnable() {
            private long loops;
            private int completed;

            @Override
            public void run() {
                try {
                    startGate.await();
                    while (completed < queueCount) {
                        process(loops++);
                    }
                    endGate.countDown();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            private void process(final long x) {
                final int idx = (int) (x % queueCount);
                long cnt = cnts[idx];

                if (cnt == -1) return;

                if (cnt >= operations) {
                    cnts[idx] = -1;
                    completed++;
                    return;
                }

                final QBufferProducer<String[]> producer = producers[idx];

                final long s = producer.begin();
                if (s == 0) {
                    return;
                }

                for (int y = 0; y < s; y++) {
                    final String[] arr = new String[arraySize];
                    for (int z = 0; z < arraySize; z++) {
                        arr[z] = object;
                    }

                    producer.produce(arr);
                }

                cnt += (producer.lazyMixCommit() * arraySize);
                cnts[idx] = cnt;
            }
        }).start();

        final long start = System.nanoTime();
        startGate.countDown();
        endGate.await();
        final long stop = System.nanoTime();

        stats("qbuffer-" + queueCount, operations * queueCount, stop - start);
    }

    private void qbufferUnitTest() throws Exception {
        final QBuffer<Long> queue = new QBuffer<>(capacity, batchSize);

        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(2);

        new Thread(new Runnable() {
            private final QBufferConsumer<Long> consumer = queue.consumer();
            private long cnt;

            @Override
            public void run() {
                try {
                    startGate.await();
                    while (cnt < operations) {
                        process();
                    }
                    endGate.countDown();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            private void process() {
                final long s = consumer.begin();
                if (s == 0) {
                    Thread.yield();
                    return;
                }

                for (int y = 0; y < s; y++) {
                    final long value = consumer.consume();
                    if (value != cnt) {
                        throw new IllegalStateException(value + " != " + cnt);
                    }
                    cnt++;
                }
                consumer.lazyMixCommit();
            }
        }).start();

        new Thread(new Runnable() {
            private final QBufferProducer<Long> producer = queue.producer();
            private long cnt;

            @Override
            public void run() {
                try {
                    startGate.await();
                    while (cnt < operations) {
                        process();
                    }
                    endGate.countDown();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            private void process() {
                final long s = producer.begin();
                if (s == 0) {
                    Thread.yield();
                    return;
                }

                for (int y = 0; y < s; y++) {
                    producer.produce(cnt);
                    cnt++;
                }
                producer.lazyMixCommit();
            }
        }).start();

        final long start = System.nanoTime();
        startGate.countDown();
        endGate.await();
        final long stop = System.nanoTime();

        stats("qbuffer-unit", operations, stop - start);
    }
}
