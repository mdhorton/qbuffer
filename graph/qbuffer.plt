#!/usr/bin/gnuplot
reset
set terminal png size 600,450

set key left top Left reverse box
set ylabel "Millions of Items / Second"
set xlabel "Batch Size"

set title "QBuffer Performance"
set grid

set logscale x
set style data linespoints

avg(ops, nanos) = (ops / nanos) * 1000

plot "qbuffer.dat" using 1:(avg($2,$3)) title "1 Queue",  \
     ""            using 1:(avg($4,$5)) title "2 Queues", \
     ""            using 1:(avg($6,$7)) title "3 Queues"
