package com.sharedmemory.face.matching;

public final class FaceVectorMath {

    private FaceVectorMath() {
    }

    public static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Embedding dimension mismatch: " + a.length + " vs " + b.length);
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static float[] l2NormalizedCentroid(java.util.List<float[]> embeddings) {
        int dim = embeddings.get(0).length;
        double[] sum = new double[dim];
        for (float[] embedding : embeddings) {
            for (int i = 0; i < dim; i++) {
                sum[i] += embedding[i];
            }
        }
        float[] mean = new float[dim];
        for (int i = 0; i < dim; i++) {
            mean[i] = (float) (sum[i] / embeddings.size());
        }
        double norm = 0;
        for (float v : mean) {
            norm += (double) v * v;
        }
        norm = Math.sqrt(norm);
        float[] centroid = new float[dim];
        for (int i = 0; i < dim; i++) {
            centroid[i] = norm == 0 ? 0 : (float) (mean[i] / norm);
        }
        return centroid;
    }
}
