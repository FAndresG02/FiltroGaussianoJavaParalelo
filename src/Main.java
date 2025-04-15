import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Clase para aplicar el filtro gaussiano a una imagen en escala de grises usando multihilos.
 */
class FiltroGaussiano extends Thread {
    private BufferedImage grayImg, output;
    private int startY, endY;
    private float[][] gaussianKernel;
    private int kernelSize;

    /**
     * Constructor de la clase FiltroGaussiano.
     * @param grayImg Imagen original en escala de grises.
     * @param output Imagen de salida procesada.
     * @param startY Fila inicial a procesar.
     * @param endY Fila final a procesar.
     * @param kernelSize Tamaño del kernel gaussiano.
     * @param sigma Valor de sigma para generar el kernel.
     */
    public FiltroGaussiano(BufferedImage grayImg, BufferedImage output, int startY, int endY, int kernelSize, float sigma) {
        this.grayImg = grayImg;
        this.output = output;
        this.startY = startY;
        this.endY = endY;
        this.kernelSize = kernelSize;
        this.gaussianKernel = generarKernelGaussiano(kernelSize, sigma);  // Se genera el kernel gaussiano
    }

    @Override
    public void run() {
        int width = grayImg.getWidth();
        int height = grayImg.getHeight();
        int offset = kernelSize / 2;  // Desfase para centrar el kernel

        // Recorre las filas asignadas a este hilo
        for (int y = startY; y < endY; y++) {
            // Recorre cada columna de la imagen
            for (int x = 0; x < width; x++) {
                float sum = 0;

                // Aplica el kernel gaussiano sobre la vecindad de cada píxel
                for (int ky = -offset; ky <= offset; ky++) {
                    for (int kx = -offset; kx <= offset; kx++) {
                        // Coordenadas del píxel vecino, con control de bordes
                        int pixelX = Math.min(Math.max(x + kx, 0), width - 1);
                        int pixelY = Math.min(Math.max(y + ky, 0), height - 1);
                        int gray = grayImg.getRGB(pixelX, pixelY) & 0xFF;  // Solo el canal de intensidad

                        // Acumula la suma ponderada por el valor del kernel
                        sum += gray * gaussianKernel[ky + offset][kx + offset];
                    }
                }

                // Normaliza el valor, limita a [0,255] y lo asigna al píxel de salida
                int value = Math.min(Math.max((int) sum, 0), 255);
                int newRGB = (value << 16) | (value << 8) | value;  // RGB con los mismos valores (escala de grises)
                output.setRGB(x, y, newRGB);
            }
        }
    }

    /**
     * Genera un kernel gaussiano 2D normalizado.
     * tamaño Tamaño del kernel (debe ser impar).
     * sigma Valor de desviación estándar (controla el desenfoque).
     */
    public static float[][] generarKernelGaussiano(int tamaño, float sigma) {
        float[][] kernel = new float[tamaño][tamaño];
        int offset = tamaño / 2;
        float suma = 0;

        // Calcula los valores del kernel gaussiano
        for (int y = -offset; y <= offset; y++) {
            for (int x = -offset; x <= offset; x++) {
                float valor = (float) (1 / (2 * Math.PI * sigma * sigma) *
                        Math.exp(-(x * x + y * y) / (2 * sigma * sigma)));
                kernel[y + offset][x + offset] = valor;
                suma += valor;
            }
        }

        // Normaliza el kernel para que la suma total sea 1
        for (int y = 0; y < tamaño; y++) {
            for (int x = 0; x < tamaño; x++) {
                kernel[y][x] /= suma;
            }
        }

        return kernel;
    }
}

/*
 * Clase principal que ejecuta el procesamiento de la imagen utilizando múltiples hilos.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Runtime runtime = Runtime.getRuntime();

        runtime.gc();  // Solicita recolección de basura para medición más precisa
        long memoriaInicial = runtime.totalMemory() - runtime.freeMemory();
        long tiempoInicio = System.nanoTime();  // Medición de tiempo

        // Carga la imagen en escala de grises (se asume que ya está en ese formato)
        BufferedImage grayImage = ImageIO.read(new File("C:\\Users\\andre\\Documents\\TRABAJO\\Tarea en clase - Filtro\\img.jpg"));
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();

        // Imagen de salida con el mismo tamaño
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Número de hilos para dividir el procesamiento
        int numThreads = 16;
        int part = height / numThreads;
        Thread[] gaussianThreads = new Thread[numThreads];

        int kernelSize = 61;      // tamaño del kernel
        float sigma = 10.0f;      // Desviación estándar

        // Crea y lanza los hilos para aplicar el filtro gaussiano por secciones
        for (int i = 0; i < numThreads; i++) {
            int startY = i * part;
            int endY = (i == numThreads - 1) ? height : startY + part;  // Último hilo cubre hasta el final
            gaussianThreads[i] = new FiltroGaussiano(grayImage, output, startY, endY, kernelSize, sigma);
            gaussianThreads[i].start();
        }

        // Espera a que todos los hilos terminen
        for (Thread t : gaussianThreads) {
            t.join();
        }

        long tiempoFin = System.nanoTime();
        long memoriaFinal = runtime.totalMemory() - runtime.freeMemory();

        // Guarda la imagen procesada
        ImageIO.write(output, "jpg", new File("C:\\Users\\andre\\Documents\\TRABAJO\\Tarea en clase - Filtro\\Imagen61.jpg"));
        System.out.println("Filtro gaussiano aplicado de forma Paralelo.");

        // Reporta tiempo de ejecución y uso de memoria
        long tiempoTotalMs = (tiempoFin - tiempoInicio) / 1_000_000;
        long memoriaUsadaKb = (memoriaFinal - memoriaInicial) / 1024;

        System.out.println("Tiempo de ejecución: " + tiempoTotalMs + " ms");
        System.out.println("Memoria utilizada: " + memoriaUsadaKb + " KB");
    }
}
