package com.texocoyotl.jobs;

import com.google.codelabs.migratingtojobs.shared.CatalogListActivity;

public class JobsCatalogListActivity extends CatalogListActivity {
    @Override
    protected void inject() {
        JobsGlobalState.get(getApplication()).inject(this);
    }
}
