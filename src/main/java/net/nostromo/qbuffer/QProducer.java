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

public abstract class QProducer<E> extends QParticipant<E> {

    public QProducer(final E[] data, final AtomicLong head, final AtomicLong tail, final int batchSize) {
        super(data, head, tail, batchSize);
    }

    // head is the queue head for the producer
    long calcOpsCapacity() {
        return data.length - (ops - head.get());
    }
}
