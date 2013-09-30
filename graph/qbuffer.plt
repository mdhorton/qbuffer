#!/usr/bin/gnuplot
reset
set terminal png size 600,450

set key outside left bottom horizontal
set ylabel "millions of items / second"
set xlabel "batch size"

set title "QBuffer Performance"
set grid

set logscale x
set style data linespoints

avg(ops, nanos) = (ops / nanos) * 1000

plot "qbuffer.dat" using 1:(avg($2,$3)) title "1 queue",  \
     ""            using 1:(avg($4,$5)) title "2 queues", \
     ""            using 1:(avg($6,$7)) title "3 queues"
