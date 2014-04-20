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
 * The consumer side object of the QBuffer queue.
 *
 * @param <E> the type of items held in this queue
 */
public class QBufferConsumer<E> extends QBufferParticipant<E> {

    // see the QBufferParticipant constructor for more info
    protected QBufferConsumer(final E[] data, final AtomicLong tail, final AtomicLong head, final AtomicBoolean active,
            final int batchSize) {
        // head is the queue tail for the consumer
        super(data, tail, head, active, batchSize);
    }

    /**
     * From the consumer's perspective this is the same as the size of the queue.
     *
     * @return the number of items that can be removed from the queue
     */
    @Override
    long availableOperations() {
        return size();
    }

    /**
     * The queue size is calculated by subtracting the number of removals from the number of adds.
     * <p>
     * From the consumer's perspective the AtomicLong head variable represents the total number of items added.
     *
     * @return the number of items currently in the queue
     */
    @Override
    public long size() {
        return head.get() - ops;
    }

    /**
     * Returns the last item in the queue.
     *
     * @return the last item in the queue
     */
    public E consume() {
        return data[(int) (ops++ & mask)];
    }
}
