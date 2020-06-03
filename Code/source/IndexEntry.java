package source;
/**
 * Index entry structure
 */
class IndexEntry extends Entry {

    private Node childNode = null;

    IndexEntry(int key, Node childNode)
    {
        super(key);
        this.childNode = childNode;
    }

    void setChildNode(Node childNode) {
        this.childNode = childNode;
    }

    Node getChildNode() {
        return childNode;
    }
}
