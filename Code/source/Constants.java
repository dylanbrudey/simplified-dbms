package source;

/**
 * Class that contains the constants in this project
 */
public class Constants {
    private final static int pageSize = 4096; //Valeur par défaut : 4096
    private final static int frameCount = 2;
    private final static int intSize = 4;

    public static int getPageSize() {
        return pageSize;
    }

    public static int getFrameCount() {
        return frameCount;
    }

    public static int getIntSize() {
        return intSize;
    }
}
