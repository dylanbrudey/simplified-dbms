package source;
/**
 * Index structure, allows more efficient searches
 */
class Index {
    private String relName;
    private int colIdx;
    private Tree tree;

    Index(String relName, int colIdx, Tree tree)
    {
        this.relName = relName;
        this.colIdx = colIdx;
        this.tree = tree;
    }

    String getRelName() {
        return relName;
    }

    int getColIdx() {
        return colIdx;
    }

    Tree getTree() {
        return tree;
    }
}
