QBuffer
--

QBuffer is a fast batch oriented lock-free queue implementation for single producer single consumer scenarios. It works best when multiple objects need to be added or removed from the queue at a time.

For example, adding 10 items at a time yields a throughput of around 100 million objects/second. Adding 100 items at a time yields 350 million objects/second.

For more details see: http://nostromo.net/projects/qbuffer

--
Mark Horton
