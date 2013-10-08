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
package net.nostromo.qbuffer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class QBuffer<E> {

    private final QBufferProducer<E> producer;
    private final QBufferConsumer<E> consumer;

    @SuppressWarnings("unchecked")
    public QBuffer(final int capacity, final int batchSize) {
        // data.length must be a power of 2
        final E[] data = (E[]) new Object[QUtil.nextPowerOf2(capacity)];

        final AtomicLong head = new AtomicLong();
        final AtomicLong tail = new AtomicLong();
        final AtomicBoolean active = new AtomicBoolean(true);

        // batchSize can't be greater than data.length
        final int actualBatchSize = Math.min(batchSize, data.length);

        producer = new QBufferProducer<>(data, head, tail, active, actualBatchSize);
        consumer = new QBufferConsumer<>(data, tail, head, active, actualBatchSize);
    }

    public QBufferProducer<E> producer() {
        return producer;
    }

    public QBufferConsumer<E> consumer() {
        return consumer;
    }
}
