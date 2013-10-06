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

import java.util.concurrent.atomic.AtomicLong;

public class QPool<E> {

    private final QPoolProducer<E> producer;
    private final QPoolConsumer<E> consumer;

    public QPool(final E[] data, final int batchSize) {
        if (data.length != QUtil.nextPowerOf2(data.length)) {
            throw new IllegalArgumentException("data.length must be a power of 2");
        }

        final AtomicLong head = new AtomicLong();
        final AtomicLong tail = new AtomicLong();

        // batchSize can't be greater that data.length
        final int actualBatchSize = Math.min(batchSize, data.length);

        producer = new QPoolProducer<>(data, head, tail, actualBatchSize);
        consumer = new QPoolConsumer<>(data, tail, head, actualBatchSize);
    }

    public QPoolProducer<E> producer() {
        return producer;
    }

    public QPoolConsumer<E> consumer() {
        return consumer;
    }
}
