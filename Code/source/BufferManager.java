package source;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Class that manage the page replacement issue and a efficient decision taker
 */
class BufferManager {
    private static BufferManager instance = null;
    private ArrayList<Frame> frameArray;
    private int frameIndex = -1;

    private BufferManager()
    {
        this.frameArray = new ArrayList<>(0);
    }

    static BufferManager getInstance()
    {
        if(instance == null)
            instance = new BufferManager();
        return instance;
    }

    void reset()
    {
        frameArray.clear();
        frameIndex = -1;
    }

    /**
     * Answer a page request from upper layers by delivering a associated buffer
     * @param pageId pageId of the requested page
     * @return  buffer filled with data from the page requested
     */
    ByteBuffer getPage(PageId pageId)
    {
        boolean isNewFrame = false;
        //"soon to be" replaced frame
        Frame replacedFrame;
        //if the page is already in a frame
        boolean pageFound = this.findPage(pageId);
        //Frame (Index) chosen in comparePage method if found
        if(!pageFound) {
            //Look if there is space available for a new frame
            if (frameArray.size() < Constants.getFrameCount()) {
                frameArray.add(new Frame(pageId));
                frameIndex = frameArray.size() - 1;
                isNewFrame = true;
            }
            //We have to replace a frame (page and buffer content) by a new one -> Clock
            else {
                //set new frameIndex
                this.getClockChoice();
                //write on disk last page of the frame if modified
                replacedFrame = frameArray.get(frameIndex);
                if (replacedFrame.isValdirty()) {
                    DiskManager.getInstance().writePage(replacedFrame.getPageId(), replacedFrame.getBuffer());
                }
                //Replace last frame by new one with the wanted page id
                this.replaceFrame(frameIndex, pageId);
                isNewFrame = true;
            }
        }
        this.loadContents(frameIndex, isNewFrame);
        return frameArray.get(frameIndex).getBuffer();
    }

    /**
     * free the page once its use by the upper layer has been fulfilled
     * @param pageId    PageId
     * @param valdirty  true if modification has been made on the buffer, else false
     */
    void freePage(PageId pageId, boolean valdirty)
    {
        if(findPage(pageId))
        {
            frameArray.get(frameIndex).setPinCount(frameArray.get(frameIndex).getPinCount() - 1);
            if(frameArray.get(frameIndex).getPinCount() == 0)
            {
                frameArray.get(frameIndex).setRefbit(1);
            }
            if(!frameArray.get(frameIndex).isValdirty())
                frameArray.get(frameIndex).setValdirty(valdirty);
        }
    }

    /**
     * Manage the page left in the buffers, save them into the files if needed, and reset the frames
     */
    void flushAll()
    {
        for(Frame frame : frameArray)
        {
            if(frame.isValdirty())
                DiskManager.getInstance().writePage(frame.getPageId(),frame.getBuffer());
        }
        frameArray.clear();
    }

    /**
     * look for a page in the list of frames and see if it already exists
     * @param pageId PageId of page we are looking for
     * @return boolean true if found, else false
     */
    private boolean findPage(PageId pageId)
    {
        boolean isEqual = false;
        if(!frameArray.isEmpty())
        {
            for(int i = 0; i < frameArray.size() && !isEqual; i++)
            {
                PageId refPageId = frameArray.get(i).getPageId();
                if(pageId.equals(refPageId))
                {
                    isEqual = true;
                    frameIndex = i;
                }
            }
        }
        return isEqual;
    }

    /**
     * Apply the clock procedure on our frames list
     */
    private void getClockChoice()
    {
        ArrayList<Frame> chosenFrames = frameArray;
        frameIndex = -1;
        while(frameIndex == -1)
        {
            for(int i = 0; i < Constants.getFrameCount(); i++)
            {
                Frame frame = chosenFrames.get(i);
                if(frame.getPinCount() == 0)
                {
                    if(frame.getRefbit() == 0)
                    {
                        frameIndex = i;
                        break;
                    }
                    else
                        chosenFrames.get(i).setRefbit(0);
                }
            }
        }
    }

    /**
     * Load contents of the selected frame, new or not
     * @param frameIndex    index of the frame in the list
     * @param isNewFrame    true if the page were not in the frame list
     */
    private void loadContents(int frameIndex, boolean isNewFrame)
    {
        frameArray.get(frameIndex).setPinCount(frameArray.get(frameIndex).getPinCount()+1);
        if(isNewFrame)
            frameArray.get(frameIndex).setBuffer(DiskManager.getInstance().readPage(frameArray.get(frameIndex).getPageId(),frameArray.get(frameIndex).getBuffer()));
        frameArray.set(frameIndex,frameArray.get(frameIndex));
        frameArray.get(frameIndex).getBuffer().rewind();
    }

    /**
     * Replace an unused frame by a new one
     * @param frameIndex    index of the frame in the list
     * @param pageId        pageId
     */
    private void replaceFrame(int frameIndex, PageId pageId)
    {
        frameArray.set(frameIndex,new Frame(pageId));
    }
}
