/*
 * Copyright (C) 2012-2018 The Android Money Manager Ex Project Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.money.manager.ex.transactions;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.money.manager.ex.Constants;
import com.money.manager.ex.R;
import com.money.manager.ex.common.Calculator;
import com.money.manager.ex.common.CategoryListActivity;
import com.money.manager.ex.common.CommonSplitCategoryLogic;
import com.money.manager.ex.common.MmxBaseFragmentActivity;
import com.money.manager.ex.core.Core;
import com.money.manager.ex.core.MenuHelper;
import com.money.manager.ex.core.TransactionTypes;
import com.money.manager.ex.database.ISplitTransaction;
import com.money.manager.ex.datalayer.TagRepository;
import com.money.manager.ex.datalayer.TaglinkRepository;
import com.money.manager.ex.domainmodel.SplitCategory;
import com.money.manager.ex.domainmodel.Tag;
import com.money.manager.ex.domainmodel.Taglink;
import com.money.manager.ex.transactions.events.AmountEntryRequestedEvent;
import com.money.manager.ex.transactions.events.CategoryRequestedEvent;
import com.money.manager.ex.transactions.events.SplitItemRemovedEvent;
import com.money.manager.ex.transactions.events.TagRequestedEvent;

import org.greenrobot.eventbus.Subscribe;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import info.javaperformance.money.Money;
import info.javaperformance.money.MoneyFactory;

public class SplitCategoriesActivity
    extends MmxBaseFragmentActivity {

    public static final String KEY_SPLIT_TRANSACTION = "SplitCategoriesActivity:ArraysSplitTransaction";
    public static final String KEY_SPLIT_TRANSACTION_DELETED = "SplitCategoriesActivity:ArraysSplitTransactionDeleted";
    public static final String KEY_TRANSACTION_TYPE = "SplitCategoriesActivity:TransactionType";
    public static final String KEY_DATASET_TYPE = "SplitCategoriesActivity:DatasetType";
    public static final String KEY_CURRENCY_ID = "SplitCategoriesActivity:CurrencyId";

    public static final String INTENT_RESULT_SPLIT_TRANSACTION = "SplitCategoriesActivity:ResultSplitTransaction";
    public static final String INTENT_RESULT_SPLIT_TRANSACTION_DELETED = "SplitCategoriesActivity:ResultSplitTransactionDeleted";

    private static final int REQUEST_PICK_CATEGORY = 1;

    /**
     * The name of the entity to create when adding split transactions.
     * Needed to distinguish between SplitCategory and SplitRecurringCategory.
     */
    private String entityTypeName = null;
    private ArrayList<ISplitTransaction> mSplitDeleted = null;
    private SplitCategoriesAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private final int amountRequestOffset = 1000; // used to offset the request number for Amounts.

    public ArrayList<Taglink> mTaglinks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new SplitCategoriesAdapter();

        handleIntent();

        // restore collections
        if (savedInstanceState != null) {
            mAdapter.splitTransactions = Parcels.unwrap(savedInstanceState.getParcelable(KEY_SPLIT_TRANSACTION));
            mSplitDeleted = Parcels.unwrap(savedInstanceState.getParcelable(KEY_SPLIT_TRANSACTION_DELETED));
        }

        // If this is a new split (no existing split categories), then create the first one.
        if(mAdapter.splitTransactions == null || mAdapter.splitTransactions.isEmpty()) {
            addSplitTransaction();
        }

        setContentView(R.layout.activity_split_categories);

        mRecyclerView = findViewById(R.id.splitsRecyclerView);

        setDisplayHomeAsUpEnabled(true);

        initRecyclerView();

        findViewById(R.id.fab).setOnClickListener(view -> addSplitTransaction());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        new MenuHelper(this, menu).addSaveToolbarIcon();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MenuHelper.save) {
            return onActionDoneClick();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((resultCode != Activity.RESULT_OK) || (data == null)) return;

        if (requestCode == REQUEST_PICK_CATEGORY) {
            long categoryId = data.getLongExtra(CategoryListActivity.INTENT_RESULT_CATEGID, Constants.NOT_SET);
            long subcategoryId = data.getLongExtra(CategoryListActivity.INTENT_RESULT_SUBCATEGID, Constants.NOT_SET);
            int location = data.getIntExtra(CategoryListActivity.KEY_REQUEST_ID, Constants.NOT_SET_INT);

            ISplitTransaction split = mAdapter.splitTransactions.get(location);
            split.setCategoryId(categoryId);

            mAdapter.notifyItemChanged(location);
        }

        if (requestCode >= amountRequestOffset) {
            int id = requestCode - amountRequestOffset;
            Money amount = Calculator.getAmountFromResult(data);
            onAmountEntered(id, amount);
        }
    }

    public boolean onActionDoneClick() {
        List<ISplitTransaction> allSplitTransactions = mAdapter.splitTransactions;
        Core core = new Core(this);
        Money total = MoneyFactory.fromString("0");

        // Validate Category.
        for (int i = 0; i < allSplitTransactions.size(); i++) {
            ISplitTransaction splitTransaction = allSplitTransactions.get(i);
            if (splitTransaction.getCategoryId() == Constants.NOT_SET) {
                core.alert(R.string.error_category_not_selected);
                return false;
            }

            total = total.add(splitTransaction.getAmount());
        }

        // total amount must not be negative.
        if (total.toDouble() < 0) {
            core.alert(R.string.split_amount_negative);
            return false;
        }

        Intent result = new Intent();
        result.putExtra(INTENT_RESULT_SPLIT_TRANSACTION, Parcels.wrap(allSplitTransactions));
        result.putExtra(INTENT_RESULT_SPLIT_TRANSACTION_DELETED, Parcels.wrap(mSplitDeleted));
        setResult(RESULT_OK, result);
        finish();

        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(KEY_SPLIT_TRANSACTION, Parcels.wrap(mAdapter.splitTransactions));

        if (mSplitDeleted != null) {
            outState.putParcelable(KEY_SPLIT_TRANSACTION_DELETED, Parcels.wrap(mSplitDeleted));
        }
    }

    /*
        Events
     */

    @Subscribe
    public void onEvent(SplitItemRemovedEvent event) {
        onRemoveItem(event.entity);
    }

    @Subscribe
    public void onEvent(AmountEntryRequestedEvent event) {
        Calculator.forActivity(this)
                .currency(mAdapter.currencyId)
                .amount(event.amount)
                .show(event.requestId + amountRequestOffset);
    }

    @Subscribe
    public void onEvent(CategoryRequestedEvent event) {
        showCategorySelector(event.requestId);
    }

    @Subscribe
    public void onEvent(TagRequestedEvent event) {
        showTagSelector(event.requestId, event.field);
    }

    /*
        Private
     */

    private void addSplitTransaction() {
        ISplitTransaction entity = SplitItemFactory.create(this.entityTypeName, mAdapter.transactionType);

        mAdapter.splitTransactions.add(entity);

        int position = mAdapter.splitTransactions.size() - 1;
        mAdapter.notifyItemInserted(position);

        if (mRecyclerView != null) {
//            mRecyclerView.smoothScrollToPosition(position);
            mRecyclerView.scrollToPosition(position);
        }
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (intent == null) return;

        this.entityTypeName = intent.getStringExtra(KEY_DATASET_TYPE);

        int transactionType = intent.getIntExtra(KEY_TRANSACTION_TYPE, 0);
        mAdapter.transactionType = TransactionTypes.values()[transactionType];

        mAdapter.currencyId = intent.getLongExtra(KEY_CURRENCY_ID, Constants.NOT_SET);

        List<ISplitTransaction> splits = Parcels.unwrap(intent.getParcelableExtra(KEY_SPLIT_TRANSACTION));
        if (splits != null) {
            mAdapter.splitTransactions = splits;
        }
        mSplitDeleted = Parcels.unwrap(intent.getParcelableExtra(KEY_SPLIT_TRANSACTION_DELETED));
    }

    private void initRecyclerView() {
//        mRecyclerView = (RecyclerView) findViewById(R.id.splitsRecyclerView);
        if (mRecyclerView == null) return;

        // adapter
        mRecyclerView.setAdapter(mAdapter);

        // layout manager - LinearLayoutManager
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Support for swipe-to-dismiss
        ItemTouchHelper.Callback swipeCallback = new SplitItemTouchCallback(mAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(swipeCallback);
        touchHelper.attachToRecyclerView(mRecyclerView);
    }

    private void onAmountEntered(int requestId, Money amount) {
        if (amount.toDouble() < 0) {
            showInvalidAmountDialog();
            return;
        }
        // The amount is always positive, ensured by the validation above.

        ISplitTransaction split = mAdapter.splitTransactions.get(requestId);

        Money adjustedAmount = CommonSplitCategoryLogic.getStorageAmount(mAdapter.transactionType, amount, split);
        split.setAmount(adjustedAmount);

        mAdapter.notifyItemChanged(requestId);
    }

    private void onRemoveItem(ISplitTransaction splitTransaction) {
        if (splitTransaction == null) return;

        if (mSplitDeleted == null) {
            mSplitDeleted = new ArrayList<>();
        }

        // Add item to delete. Only if not a new, non-saved split item.
        if (splitTransaction.getId() != null && splitTransaction.getId() != Constants.NOT_SET) {
            // not new split transaction
            mSplitDeleted.add(splitTransaction);
        }
    }

    private void showCategorySelector(int requestId) {
        Intent intent = new Intent(this, CategoryListActivity.class);
        intent.setAction(Intent.ACTION_PICK);

        // add id of the item that requested the category.
        intent.putExtra(CategoryListActivity.KEY_REQUEST_ID, requestId);

        startActivityForResult(intent, REQUEST_PICK_CATEGORY);
    }

    private void showTagSelector(int requestId, TextView field) {
        // we cannot use intent for tag since actually TagListActivity does not support
        // multiselection.
        TaglinkRepository repo = new TaglinkRepository(mAdapter.getContext());
        field.setText( repo.loadTagsfor( mTaglinks ) );

        TagRepository tagRepository = new TagRepository(mAdapter.getContext());
        ArrayList<Tag> tagsList = tagRepository.getAllActiveTag();
        boolean[] tagsFlag = new boolean[tagsList.size()];
        String[] tagsListString = new String[tagsList.size()];
        for (int i = 0; i < tagsList.size(); i++) {
            tagsListString[i] = tagsList.get(i).getName();
            // set default from mTagLink
            long tagId = tagsList.get(i).getId().intValue();
            if ( mTaglinks != null && mTaglinks.stream().filter(x -> x.getTagId() == tagId ).findFirst().isPresent() ) {
                tagsFlag[i] = true;
            };
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mAdapter.getContext());
        // set title
        builder.setTitle(R.string.tagsList_transactions);
        builder.setCancelable(false);
        builder.setMultiChoiceItems(tagsListString, tagsFlag,  new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                tagsFlag[i] = b;
            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Initialize string builder
                // Save also taglink, loop at mtaglink to check actual record
                for (int j = 0; j < tagsList.size(); j++) {
                    long tagId = tagsList.get(j).getId().intValue();
                    Taglink taglink ;
                    try {
                        taglink = mTaglinks.stream().filter(x -> x.getTagId() == tagId ).findFirst().get();
                    } catch ( Exception e) {
                        taglink = null;
                    }
                    if (taglink == null ) {
                        if ( ! tagsFlag[j] ) {
                            // flag off and mlink not present, nothing to do
                        } else {
                            // flag on and mlink not present, create
                            if (mTaglinks == null) mTaglinks = new ArrayList<Taglink>();
                            taglink = new Taglink();
                            taglink.setRefType(mAdapter.splitTransactions.get(requestId).getTransactionModel());
                            taglink.setRefId(mAdapter.splitTransactions.get(requestId).getId());
                            taglink.setTagId(tagId);
                            mTaglinks.add(taglink);
                        }
                    } else {
                        if ( ! tagsFlag[j] && mTaglinks != null ) {
                            // flag off and mlink is present, delete
                            mTaglinks.remove(taglink);
                        } else {
                            // flag on and mlink present  nothing
                        }
                    }
                }
                // TODO update UI field
                mAdapter.splitTransactions.get(requestId).setTags(mTaglinks);
//                displayTags();
                field.setText( repo.loadTagsfor( mTaglinks ) );

            }
        });

        builder.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // dismiss dialog
                dialogInterface.dismiss();
            }
        });

        builder.setNeutralButton("Clear All", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // use for loop
                for (int j = 0; j < tagsListString.length; j++) {
                    // remove all selection
                    tagsFlag[j] = false;
                }
            }
        });

        builder.show();
    }

    private void showInvalidAmountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.error)
                .setMessage(R.string.error_amount_must_be_positive)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    // Handle positive button click if needed
                    dialog.dismiss();
                })
                .show();
    }
}
