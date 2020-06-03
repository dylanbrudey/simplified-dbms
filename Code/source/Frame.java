package source;

import java.nio.ByteBuffer;

/**
 * Class that contains the frame structure along with its buffer
 */
public class Frame {

    private ByteBuffer buffer;
    private PageId pageId;
    private int  pinCount;
    private boolean valdirty;
    private int refbit;

    Frame(PageId pageId)
    {
        this.buffer = ByteBuffer.allocateDirect(Constants.getPageSize());
        this.pageId = pageId;
        this.pinCount = 0;
        this.valdirty = false;
        this.refbit = 0;
    }

    void setPinCount(int pinCount) {
        this.pinCount = pinCount;
    }

    void setValdirty(boolean valdirty) {
        this.valdirty = valdirty;
    }

    void setRefbit(int refbit) { this.refbit = refbit; }


    void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    ByteBuffer getBuffer() {
        return buffer;
    }

    PageId getPageId() {
        return pageId;
    }

    int getPinCount() {
        return pinCount;
    }

    int getRefbit() {
        return refbit;
    }

    boolean isValdirty() {
        return valdirty;
    }

}
