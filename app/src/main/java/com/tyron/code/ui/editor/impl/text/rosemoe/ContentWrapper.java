package com.tyron.code.ui.editor.impl.text.rosemoe;

import androidx.annotation.NonNull;

import com.google.common.collect.Maps;
import com.tyron.editor.event.ContentEvent;
import com.tyron.editor.event.ContentListener;
import com.tyron.editor.event.impl.ContentEventImpl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.TextRange;

public class ContentWrapper extends Content implements com.tyron.editor.Content {

    private AtomicInteger sequence;
    private boolean hasCalledSuper = false;
    private long modificationStamp = 0;
    private final Map<String, Object> dataMap = Maps.newConcurrentMap();
    private final List<ContentListener> contentListeners = new CopyOnWriteArrayList<>();

    public ContentWrapper() {
        super();
        hasCalledSuper = true;
    }

    public ContentWrapper(CharSequence text) {
        super(text, true);
        hasCalledSuper = true;
    }

    @Override
    public void insert(int index, CharSequence text) {
        CharPosition pos = getIndexer().getCharPosition(index);
        insert(pos.line, pos.column, text);
    }

    @Override
    public void insert(int line, int column, CharSequence text) {
        super.insert(line, column, text);
        if (!hasCalledSuper) return;
        int offset = getCharIndex(line, column);
        updateText(this, offset, "", text, false, System.currentTimeMillis(), offset, 0, offset);
    }

    @Override
    public void replace(int start, int end, CharSequence text) {
        CharPosition startPos = getIndexer().getCharPosition(start);
        CharPosition endPos = getIndexer().getCharPosition(end);
        replace(startPos.line, startPos.column, endPos.line, endPos.column, text);
    }

    @Override
    public void delete(int start, int end) {
        CharPosition startPos = getIndexer().getCharPosition(start);
        CharPosition endPos = getIndexer().getCharPosition(end);
        delete(startPos.line, startPos.column, endPos.line, endPos.column);
    }

    @Override
    public void delete(int startLine, int columnOnStartLine, int endLine, int columnOnEndLine) {
        int startOffset = getCharIndex(startLine, columnOnStartLine);
        int endOffset = getCharIndex(endLine, columnOnEndLine);
        CharSequence oldString = subSequence(startOffset, endOffset);
        super.delete(startLine, columnOnStartLine, endLine, columnOnEndLine);
        if (!hasCalledSuper) return;
        updateText(this, startOffset, oldString, "", false, System.currentTimeMillis(),
                startOffset, endOffset - startOffset, startOffset);
    }

    private AtomicInteger getSequence() {
        if (sequence == null) sequence = new AtomicInteger(0);
        return sequence;
    }

    protected void updateText(@NonNull CharSequence text, int offset, @NonNull CharSequence oldString, @NonNull CharSequence newString, boolean wholeTextReplaced, long newModificationStamp, int initialStartOffset, int initialOldLength, int moveOffset) {
        ContentEvent event = new ContentEventImpl(this, offset, oldString, newString, modificationStamp, wholeTextReplaced, initialStartOffset, initialOldLength, moveOffset);
        getSequence().incrementAndGet();
        changedUpdate(event, newModificationStamp, this);
    }

    protected void changedUpdate(@NonNull ContentEvent event, long newModificationStamp, @NonNull CharSequence prevText) {
        for (ContentListener contentListener : contentListeners) {
            contentListener.contentChanged(event);
        }
    }

    @Override
    public void setData(String key, Object object) { dataMap.put(key, object); }

    @Override
    public Object getData(String key) { return dataMap.get(key); }

    @Override
    public void addContentListener(@NonNull ContentListener listener) { contentListeners.add(listener); }

    @Override
    public void removeContentListener(@NonNull ContentListener listener) { contentListeners.remove(listener); }

    @Override
    public void setModificationStamp(long stamp) { modificationStamp = stamp; }

    @Override
    public long getModificationStamp() { return modificationStamp; }

    @Override
    public boolean canRedo() { return super.canRedo(); }

    @Override
    public void redo() { 
        super.redo(); 
    }

    @Override
    public boolean canUndo() { return super.canUndo(); }

    @Override
    public TextRange undo() {
        return super.undo(); 
    }
}
