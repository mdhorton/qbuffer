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

/**
 * The producer side object of the QBuffer queue.
 *
 * @param <E> the type of items held in this queue
 */
public class QBufferProducer<E> extends QParticipant<E> {

    // see the QParticipant constructor for more info
    protected QBufferProducer(final E[] data, final AtomicLong head, final AtomicLong tail, final AtomicBoolean active,
            final int batchSize) {
        // head is the queue head for the producer
        super(data, head, tail, active, batchSize);
    }

    /**
     * From the producer's perspective this is the queue size subtracted from the length of the backing data array.
     *
     * @return the number of items that can be added to the queue
     */
    @Override
    long availableOperations() {
        return data.length - size();
    }

    /**
     * The queue size is calculated by subtracting the number of removals from the number of adds.
     * <p>
     * From the producer's perspective the AtomicLong head variable represents the total number of items removed.
     *
     * @return the number of items currently in the queue
     */
    @Override
    public long size() {
        return ops - head.get();
    }

    /**
     * Add an item to the end of the queue.
     *
     * @param e the item to be added
     */
    public void produce(final E e) {
        data[(int) (ops++ & mask)] = e;
    }
}
