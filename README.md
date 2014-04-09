QBuffer
--

QBuffer is a fast batch oriented queue implementation for single producer/single consumer scenarios.  It works best when multiple items need to be added or removed from the queue at a time.

For example, adding 10 items at a time yields a throughput of 100 million items/second.  Adding 100 items at a time yields 350 million items/second.

For more details see: http://nostromo.net/projects/qbuffer

--
Mark Horton
