QBuffer
--

QBuffer is a fast batch oriented queue implementation for single producer/single consumer scenarios.  It works best when multiple items need to be added or removed from the queue.

For example, adding 10 items at a time yields a throughput of 100 million items/second.  Adding 100 items at a time yields 400 million items/second.

For a brief writeup see: http://blog.nostromo.net/2013/09/qbuffer.html

--
Mark Horton
