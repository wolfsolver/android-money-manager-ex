/******************************************************************************
 * .
 *  2016 (C) Copyright Open-RnD Sp. z o.o.
 * .
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * .
 *  http://www.apache.org/licenses/LICENSE-2.0
 * .
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package com.money.manager.ex.nestedcategory.multilevellistview;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * Base adapter to be used for MultiLevelListView.
 */
public abstract class MultiLevelListAdapter {

    private BaseMultiLevelListFragment mView;

    private Node mRoot = new Node();
    private List<Node> mFlatItems = new ArrayList<>();
    private List<Object> mSourceData = new ArrayList<>();
    private ProxyAdapter mProxyAdapter = new ProxyAdapter();

    /**.
     * Indicates if object is expandable.
     *
     * @param object The object.
     * @return true if object is expandable, false otherwise.
     */
    protected abstract boolean isExpandable(Object object);

    /**
     * Gets list of object's sub-items.
     *
     * Called only for expandable objects.
     *
     * @param object The object.
     * @return List of sub-objects. Null is possible.
     */
    protected abstract List<?> getSubObjects(Object object);

    /**
     *
     * @param object
     * @return
     */
    protected abstract Object getParent(Object object);

    /**
     * Gets view configured to display the object.
     *
     * @param object The object.
     * @param convertView The view that can be reused if possible. Null value if not available.
     * @param itemInfo The InfoItem object with information about item location in MultiLevelListView.
     * @return The view that reflects the object.
     */
    protected abstract View getViewForObject(Object object, View convertView, ItemInfo itemInfo, int pos);

    /**
     * Sets initial data items to be displayed in attached MultiLevelListView.
     *
     * @param dataItems The list with data items.
     */
    public void setDataItems(List<?> dataItems) {
        setDataItems(dataItems, null);
    }

    /**
     * Sets initial data items to be displayed in attached MultiLevelListView and expand passed hierarchy of nodes.
     *
     * @param dataItems The list with data items.
     */
    public void setDataItems(List<?> dataItems, Stack<?> expandItems) {
        checkState();

        mSourceData = new ArrayList<>();
        mSourceData.addAll(dataItems);

        if (expandItems != null) {
            mRoot.setSubNodes(createNodeListFromDataItems(mSourceData, mRoot, expandItems));
        } else {
            mRoot.setSubNodes(createNodeListFromDataItems(mSourceData, mRoot));
        }
        notifyDataSetChanged();
    }

    /**
     * Notifies adapter that data set changed.
     */
    public void notifyDataSetChanged() {
        checkState();

        mFlatItems = createItemsForCurrentStat();
        mProxyAdapter.notifyDataSetChanged();
    }

    /**
     * Reloads data. Method is causing nodes recreation.
     */
    void reloadData() {
        setDataItems(mSourceData);
    }

    /**
     * Throws IllegalStateException if adapter is not attached to view.
     */
    private void checkState() {
        if (mView == null) {
            throw new IllegalStateException("Adapter not connected");
        }
    }

    /**
     * Creates list of nodes for data items provided to adapter.
     *
     * @param dataItems List of objects for which nodes have to be created.
     * @param parent Node that is a parent for nodes created for data items.
     * @return List with nodes.
     */
    private List<Node> createNodeListFromDataItems(List<?> dataItems, Node parent) {
        List<Node> result = new ArrayList<>();
        if (dataItems != null) {
            for (Object dataItem : dataItems) {
                if (parent == mRoot) {
                    mRoot.setObject(getParent(dataItem));
                }
                boolean isExpandable = isExpandable(dataItem);
                Node node = new Node(dataItem, parent);
                node.setExpandable(isExpandable);
                if (mView.isAlwaysExpanded() && isExpandable) {
                    node.setSubNodes(createNodeListFromDataItems(getSubObjects(node.getObject()), node));
                }
                result.add(node);
            }
        }
        return result;
    }

    /**
     * Creates list of nodes for data items provided to adapter and expand passed hierarchy of nodes.
     *
     * @param dataItems
     * @param parent
     * @param expandItems
     * @return
     */
    private List<Node> createNodeListFromDataItems(List<?> dataItems, Node parent, Stack<?> expandItems) {
        Object expandItem = (expandItems != null && expandItems.size() > 1) ? expandItems.pop() : null;
        List<Node> result = new ArrayList<>();
        if (dataItems != null) {
            for (Object dataItem : dataItems) {
                if (parent == mRoot) {
                    mRoot.setObject(getParent(dataItem));
                }
                boolean isExpandable = isExpandable(dataItem);
                Node node = new Node(dataItem, parent);
                node.setExpandable(isExpandable);
                if (isExpandable && (mView.isAlwaysExpanded() || dataItem == expandItem)) {
                    if (dataItem == expandItem) {
                        node.setSubNodes(createNodeListFromDataItems(getSubObjects(node.getObject()), node, expandItems));
                    } else {
                        node.setSubNodes(createNodeListFromDataItems(getSubObjects(node.getObject()), node));
                    }
                }
                result.add(node);
            }
        }
        return result;
    }

    /**
     * Maps current items hierarchy into flat list.
     *
     * @return Items flat list.
     */
    private List<Node> createItemsForCurrentStat() {
        List<Node> result = new ArrayList<>();
        collectItems(result, mRoot.getSubNodes());
        return result;
    }

    /**
     * Adds recurrently nodes and their sub-nodes to provided list.
     *
     * @param result Output parameter with flat list of items.
     * @param nodes Nodes list.
     */
    private void collectItems(List<Node> result, List<Node> nodes) {
        if (nodes != null) {
            for (Node node : nodes) {
                result.add(node);
                collectItems(result, node.getSubNodes());
            }
        }
    }

    /**
     * Gets currently displayed list of items.
     *
     * @return List items.
     */
    List<Node> getFlatItems() {
        return mFlatItems;
    }

    /**
     * Unregisters adapter in MultiLevelListView.
     *
     * @param view The view to unregister.
     * @throws IllegalArgumentException if adapter is not registered in the view.
     */
    void unregisterView(BaseMultiLevelListFragment view) {
        if (mView != view) {
            throw new IllegalArgumentException("Adapter not connected");
        }

        if (mView == null) {
            return;
        }

        mView.getListView().setAdapter(null);
        mView = null;
    }

    /**
     * Register adapter in MultiLevelListView.
     *
     * @param view The view to register.
     * @throws IllegalArgumentException if adapter is registered in different view.
     */
    void registerView(BaseMultiLevelListFragment view) {
        if ((mView != null) && (mView != view)) {
            throw new IllegalArgumentException("Adapter already connected");
        }

        if (view == null) {
            return;
        }

        mView = view;
        mView.getListView().setAdapter(mProxyAdapter);
    }

    /**
     * Extends node.
     *
     * Adds sub-nodes to the node.
     *
     * @param node The node.
     * @param nestTyp NestType value.
     */
    public void extendNode(Node node, NestType nestTyp) {
        node.setSubNodes(createNodeListFromDataItems(getSubObjects(node.getObject()), node));
        if (nestTyp == NestType.SINGLE) {
            clearPathToNode(node);
        }
        notifyDataSetChanged();
    }

    /**
     * Extends node and subnodes (recursively).
     *
     * Adds sub-nodes to the nodes.
     *
     * @param node The node.
     * @param nestTyp NestType value.
     */
    public void extendNodeSubnodes(Node node, NestType nestTyp) {
        extendNode(node);
        if (nestTyp == NestType.SINGLE) {
            clearPathToNode(node);
        }
        notifyDataSetChanged();
    }

    /**
     *
     * @param pos
     * @param nestTyp
     */
    public void extendNodeSubnodes(int pos, NestType nestTyp) {
        Node node = mFlatItems.get(pos);
        if (node != null) {
            extendNodeSubnodes(node, nestTyp);
        }
    }

    /**
     * Only extends node and subnodes (recursively).
     *
     * Adds sub-nodes to the nodes.
     *
     * @param node The node.
     */
    private void extendNode(Node node) {
        List<Node> subNodes = createNodeListFromDataItems(getSubObjects(node.getObject()), node);
        node.setSubNodes(subNodes);
        for (Node subNode : subNodes) {
            if (subNode.isExpandable()) {
                extendNode(subNode);
            }
        }
    }

    public int extendToNode(Object nodeObj, Stack<Object> expandItems) {
        if (nodeObj == null) {
            return -1;
        }
        if (nodeObj == mRoot.getObject()) {
            return -2;
        }
        if (expandItems == null) {
            expandItems = new Stack<>();
        }

        Object nextNodeObj;
        int flatPos = getPosFromObject(nodeObj);
        if (flatPos < 0) {
            // add to stack
            expandItems.push(nodeObj);
            nextNodeObj = getParent(nodeObj);
        } else {
            if (expandItems.size() == 0) {
                // finish
                mProxyAdapter.notifyDataSetChanged();
                return flatPos;
            } else {
                // expand node
                Node node = mFlatItems.get(flatPos);
                node.setSubNodes(createNodeListFromDataItems(getSubObjects(node.getObject()), node));

                // update flat list (add new node subnodes)
                mFlatItems = createItemsForCurrentStat();
                // get from stack
                nextNodeObj = expandItems.pop();
            }
        }
        return extendToNode(nextNodeObj, expandItems);
    }

    /**
     * Collapse node.
     *
     * Clears node's sub-nodes.
     *
     * @param node The node
     */
    public void collapseNode(Node node) {
        node.clearSubNodes();
        notifyDataSetChanged();
    }

    /**
     * Toggle node is expanded/collapsed.
     * @param flatPos
     */
    public void toggleNodeExpand(int flatPos) {
        if (flatPos < 0 || flatPos >= mFlatItems.size())
            return;
        Node node = mFlatItems.get(flatPos);
        if (node == null)
            return;
        if (node.isExpanded()) {
            collapseNode(node);
        } else {
            extendNode(node, NestType.MULTIPLE);
        }
    }

    /**
     * Collapse any extended way not leading to the node.
     *
     * @param node The node.
     */
    private void clearPathToNode(Node node) {
        Node parent = node.getParent();
        if (parent != null) {
            List<Node> nodes = parent.getSubNodes();
            if (nodes != null) {
                for (Node sibling : nodes) {
                    if (sibling != node) {
                        sibling.clearSubNodes();
                    }
                }
            }
            clearPathToNode(parent);
        }
    }

    /**
     * Swap two items in flat list.
     * @param flatPos Position in flat list
     * @param nodePos Position in node
     * @param nodePos2 Position in node
     * @return
     */
    public boolean swapItems(int flatPos, int nodePos, int nodePos2) {
        if (flatPos < 0 || flatPos >= mFlatItems.size()
                || nodePos < 0 || nodePos2 < 0 || nodePos == nodePos2)
            return false;
        Node node = mFlatItems.get(flatPos);
        if (node != null && node.getParent() != null) {
            List<Node> subNodes = node.getParent().getSubNodes();
            int size = subNodes.size();
            if (nodePos < size && nodePos2 < size) {
                Collections.swap(subNodes, nodePos, nodePos2);
                notifyDataSetChanged();
                return true;
            }
        }
        return false;
    }

    /**
     * Add item to some node.
     * @param flatPos position of node on which need to create a new node. May be parent or adjacent
     * @param isSubNode is branch {@param flatPos} parent
     * @return
     */
    public boolean addItem(int flatPos, boolean isSubNode) {
        if (flatPos < 0 || flatPos >= mFlatItems.size())
            return false;
        Node parentNode = mFlatItems.get(flatPos);
        if (!isSubNode) {
            parentNode = (parentNode != null) ? parentNode.getParent() : mRoot;
        }
        return addItem(parentNode);
    }

    public boolean addItem(Object parentNodeObj) {
        Stack<Object> expandHierarchy = new Stack<>();
        int flatPos = extendToNode(parentNodeObj, expandHierarchy);
        if (flatPos == -2) {
            return addItem(mRoot);
        }
        return addItem(flatPos, true);
    }

    /**
     * Add item to some node.
     * @param flatPos
     * @return
     */
    public boolean addItem(int flatPos) {
        return addItem(flatPos, true);
    }

    /**
     * Add item to the root.
     * @return
     */
    public boolean addItem() {
        return addItem(mRoot);
    }

    /**
     * Update the subNodes list of {@param parentNode} to add the new node and expand it.
     * @param parentNode
     * @return
     */
    public boolean addItem(Node parentNode) {
        parentNode.setSubNodes(createNodeListFromDataItems(getSubObjects(parentNode.getObject()), parentNode));
        parentNode.setExpandable(true);
        notifyDataSetChanged();
        return true;
    }

    /**
     * Delete item.
     * @param flatPos
     * @return
     */
    public boolean deleteItem(int flatPos) {
        if (flatPos < 0 || flatPos >= mFlatItems.size())
            return false;
        Node node = mFlatItems.get(flatPos);
        if (node != null && node.getParent() != null) {
            List<Node> subNodes = node.getParent().getSubNodes();
            if (subNodes.size() > 0) {
                subNodes.remove(node);

                if (subNodes.isEmpty())
                    node.getParent().setExpandable(false);

                notifyDataSetChanged();
                return true;
            }
        }
        return false;
    }

    /**
     * Get node position by object.
     * @param nodeObj
     * @return
     */
    private int getPosFromObject(Object nodeObj) {
        for (int i = 0; i < mFlatItems.size(); i++) {
            if (mFlatItems.get(i).getObject() == nodeObj)
                return i;
        }
        return -1;
    }

    /**
     * Get node is expanded.
     * @param flatPos
     * @return
     */
    public boolean isExpanded(int flatPos) {
        if (flatPos < 0 || flatPos >= mFlatItems.size())
            return false;
        Node node = mFlatItems.get(flatPos);
        if (node != null) {
            return node.isExpanded();
        }
        return false;
    }

    /**
     *
     * @return
     */
    protected ListView getListView() {
        return mView.getListView();
    }

    /**
     * Helper class used to display created flat list of item's using Android's ListView.
     */
    private class ProxyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mFlatItems == null ? 0 : mFlatItems.size();
        }

        @Override
        public Object getItem(int i) {
            return mFlatItems.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            Node node = mFlatItems.get(i);
            return getViewForObject(node.getObject(), convertView, node.getItemInfo(), i);
        }
    }
}
