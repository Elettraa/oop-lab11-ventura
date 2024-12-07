package it.unibo.oop.workers02;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * This is an implementation using streams.
 */
public final class MultiThreadedSumMatrix implements SumMatrix {

    private final int n;

    /**
     * 
     * @param n number of workers/threads
     */
    public MultiThreadedSumMatrix(final int n) {
        this.n = n;
    }

    private static class Worker extends Thread {
        private final List<Double> flatMatr;
        private final int startpos;
        private final int nelem;
        private double res;

        /**
         * Build a new worker.
         * 
         * @param flatMatr
         *                 matrix to sum, flattened as a list of doubles
         * @param startpos
         *                 the initial position for this worker
         * @param nelem
         *                 the no. of elems to sum up for this worker
         */
        Worker(final List<Double> flatMatr, final int startpos, final int nelem) {
            super();
            this.flatMatr = flatMatr;
            this.startpos = startpos;
            this.nelem = nelem;
        }

        @Override
        @SuppressWarnings("PMD.SystemPrintln")
        public void run() {
            System.out.println("Working from position " + startpos + " to position " + (startpos + nelem - 1));
            for (int i = startpos; i < flatMatr.size() && i < startpos + nelem; i++) {
                this.res += this.flatMatr.get(i);
            }
        }

        /**
         * Returns the result of summing up the doubles within the list/flattened
         * matrix.
         * 
         * @return the sum of every element in the array
         */
        public double getResult() {
            return this.res;
        }
    }

    @Override
    public double sum(final double[][] matrix) {
        final List<Double> flattenedMat = Arrays.stream(matrix)
                .flatMapToDouble(DoubleStream::of)
                .boxed()
                .collect(Collectors.toList());

        final int size = flattenedMat.size() % n + flattenedMat.size() / n;
        /*
         * Build a stream of workers
         */
        return IntStream
                .iterate(0, start -> start + size)
                .limit(n)
                .mapToObj(start -> new Worker(flattenedMat, start, size))
                // Start them
                .peek(Thread::start)
                // Join them
                .peek(MultiThreadedSumMatrix::joinUninterruptibly)
                // Get their result and sum
                .mapToDouble(Worker::getResult)
                .sum();
    }

    @SuppressWarnings("PMD.AvoidPrintStackTrace")
    private static void joinUninterruptibly(final Thread target) {
        var joined = false;
        while (!joined) {
            try {
                target.join();
                joined = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
