package dev.tbm00.papermc.playershops64.data.structure;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.NoSuchElementException;

import dev.tbm00.papermc.playershops64.utils.StaticUtils;

// max priority queue
public class PriceQueue implements Iterable<PriceNode> {
    private final List<PriceNode> heap = new ArrayList<>();
    private final Map<UUID, Integer> uuidIndexMap = new HashMap<>();

    public PriceQueue() {}

    private PriceQueue(PriceQueue other) {
        this.heap.addAll(other.heap);
        for (int i = 0; i < heap.size(); i++)
            uuidIndexMap.put(heap.get(i).getUuid(), i);
    }

    public int size() { return heap.size(); }
    public boolean isEmpty() { return heap.isEmpty(); }
    public void clear() { heap.clear(); uuidIndexMap.clear(); }

    public void insert(PriceNode node) {
        if (node == null) throw new NullPointerException("node");
        UUID id = node.getUuid();
        if (uuidIndexMap.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate UUID: " + id);
        }
        heap.add(node);
        int idx = heap.size() - 1;
        uuidIndexMap.put(id, idx);
        siftUp(idx);
    } public void insert(UUID value, BigDecimal weight) {
        insert(new PriceNode(StaticUtils.normalizeBigDecimal(weight), value));
    }

    public PriceNode get(UUID id) {
        Integer idx = uuidIndexMap.get(id);
        return (idx == null) ? null : heap.get(idx);
    }

    public boolean contains(UUID id) { return uuidIndexMap.containsKey(id); }

    /** Update the weight for a UUID; returns false if not found. */
    public boolean update(UUID id, BigDecimal newWeight) {
        Integer idx = uuidIndexMap.get(id);
        if (idx == null) return false;
        PriceNode old = heap.get(idx);
        BigDecimal normalized = StaticUtils.normalizeBigDecimal(newWeight);
        if (old.getPrice().compareTo(normalized) == 0) return true; // no-op
        PriceNode updated = new PriceNode(normalized, id);
        heap.set(idx, updated);
        
        if (normalized.compareTo(old.getPrice()) > 0) {
            siftUp(idx);
        } else {
            siftDown(idx);
        }
        return true;
    }

    /** Delete by UUID; returns the removed node or null if not present. */
    public PriceNode delete(UUID id) {
        Integer idxObj = uuidIndexMap.get(id);
        if (idxObj == null) return null;
        int idx = idxObj;
        int last = heap.size() - 1;

        PriceNode removed = heap.get(idx);
        swap(idx, last);
        heap.remove(last);
        uuidIndexMap.remove(id);

        if (idx < heap.size()) {
            if (!siftUp(idx)) siftDown(idx);
        }
        return removed;
    }

    public PriceNode peek() { return heap.isEmpty() ? null : heap.get(0); }

    private PriceNode poll() {
        if (heap.isEmpty()) return null;
        PriceNode max = heap.get(0);
        delete(max.getUuid());
        return max;
    }

    // returns a non-destructive iterator - going in descending weight order
    @Override
    public Iterator<PriceNode> iterator() {
        PriceQueue snap = new PriceQueue(this);
        return new Iterator<PriceNode>() {
            @Override public boolean hasNext() { return !snap.isEmpty(); }
            @Override public PriceNode next() {
                PriceNode n = snap.poll();
                if (n == null) throw new NoSuchElementException();
                return n;
            }
        };
    }

    private boolean siftUp(int idx) {
        boolean moved = false;
        while (idx > 0) {
            int parent = (idx - 1) >>> 1;
            if (heap.get(idx).compareTo(heap.get(parent)) <= 0) break;
            swap(idx, parent);
            idx = parent;
            moved = true;
        }
        return moved;
    }

    private void siftDown(int idx) {
        int n = heap.size();
        while (true) {
            int left = (idx << 1) + 1;
            if (left >= n) break;
            int right = left + 1;
            int largest = left;
            if (right < n && heap.get(right).compareTo(heap.get(left)) > 0) largest = right;
            if (heap.get(idx).compareTo(heap.get(largest)) >= 0) break;
            swap(idx, largest);
            idx = largest;
        }
    }

    private void swap(int i, int j) {
        if (i == j) return;
        PriceNode a = heap.get(i);
        PriceNode b = heap.get(j);
        heap.set(i, b);
        heap.set(j, a);
        uuidIndexMap.put(b.getUuid(), i);
        uuidIndexMap.put(a.getUuid(), j);
    }
}