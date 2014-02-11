#!/usr/bin/gnuplot
reset
set terminal png size 600,450

set ylabel "Millions of Items / Second"
set xlabel "Queues"

set title "JDK Queue Performance"
set grid

set logscale x
set style data linespoints

avg(ops, nanos) = (ops / nanos) * 1000

plot "jdkqueue.dat" using 1:(avg($2,$3))
