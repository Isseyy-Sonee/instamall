package com.example.b07_project_group5;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AddOrEditProductActivity extends AppCompatActivity {
    String userId;
    String accountType;
    String storeId;
    String productId;
    String name;
    double price;
    int stock;
    String image;
    String description;
    FirebaseDatabase db;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_or_edit_product);

        Intent intent = getIntent();
        if (intent != null) {
            this.userId = intent.getStringExtra("userId");
            this.accountType = intent.getStringExtra("accountType");
            this.storeId = intent.getStringExtra("storeId");
            this.productId = "";
            this.name = intent.getStringExtra("name");
            this.price = intent.getDoubleExtra("price", 0);
            this.stock = intent.getIntExtra("stock", 0);
            this.image = intent.getStringExtra("image");
            this.description = intent.getStringExtra("description");
            this.db = FirebaseDatabase.getInstance("https://testing-7a8a5-default-rtdb.firebaseio.com/");
        }

        TextView header = findViewById(R.id.addProductHeader);
        EditText editName = findViewById(R.id.editName);
        EditText editPrice = findViewById(R.id.editPrice);
        EditText editStock = findViewById(R.id.editStock);
        EditText editImage = findViewById(R.id.editImage);
        EditText editDescription = findViewById(R.id.editDescription);
        Button buttonSubmit = findViewById(R.id.buttonSubmit);

        if (name != null) {
            editName.setText(name);
            DatabaseReference ref = db.getReference();
            ref.child("products").orderByChild("storeId").equalTo(storeId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot productSnapshot: snapshot.getChildren()) {
                            if (name.equals(productSnapshot.child("name").getValue().toString())) {
                                productId = productSnapshot.getKey();
                                header.setText(getString(R.string.edit_product_header_text));
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
        if (price != 0) { editPrice.setText(String.valueOf(price)); }
        if (stock != 0) { editStock.setText(String.valueOf(stock)); }
        if (image != null) { editImage.setText(image); }
        if (description != null) { editDescription.setText(description); }

        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the text entered by the user
                name = editName.getText().toString();
                String userInputPrice = editPrice.getText().toString();
                String userInputStock = editStock.getText().toString();
                image = editImage.getText().toString();
                description = editDescription.getText().toString();

                if (name.equals("") || userInputPrice.equals("") || userInputStock.equals("") || image.equals("") || description.equals("")) {
                    setWarningText(getString(R.string.add_product_empty_fields_warning));
                    return;
                }

                // Convert price and stock to integers
                price = Double.parseDouble(userInputPrice);
                stock = Integer.parseInt(userInputStock);

                // Write the data to the database
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("products");
                Product product = new Product(storeId, name, price, stock, image, description);
                ref.orderByChild("name").equalTo(product.getName()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot productSnapshot: snapshot.getChildren()) {
                                if (storeId.equals(productSnapshot.child("storeId").getValue().toString()) && !productId.equals(productSnapshot.getKey())) {
                                    setWarningText(getString(R.string.product_already_exists_warning));
                                    return;
                                }
                            }
                        }
                        setWarningText("");
                        if (productId.isEmpty()) {
                            String uniqueKey = ref.push().getKey();
                            ref.child(uniqueKey).setValue(product).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Toast.makeText(AddOrEditProductActivity.this, getString(R.string.add_product_successful_text), Toast.LENGTH_LONG).show();
                                    finish();
                                }
                            });
                        } else {
                            ref.child(productId).setValue(product).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Toast.makeText(AddOrEditProductActivity.this, getString(R.string.edit_product_successful_text), Toast.LENGTH_LONG).show();
                                    finish();
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
        });

        BottomNavigationView ownerBottomNavigationView = findViewById(R.id.owner_nav_menu);
        ownerBottomNavigationView.setSelectedItemId(R.id.owner_nav_menu_store);
        ownerBottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.owner_nav_menu_store) {
                    return true;
                } else if (itemId == R.id.owner_nav_menu_orders) {
                    Intent intent = new Intent(AddOrEditProductActivity.this, LoginActivity.class);
                    intent.putExtra("userId", userId);
                    intent.putExtra("accountType", accountType);
                    intent.putExtra("storeId", storeId);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (itemId == R.id.owner_nav_menu_logout) {
                    Toast.makeText(AddOrEditProductActivity.this, getString(R.string.logout_successful_text), Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(AddOrEditProductActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            }
        });
    }

    public void setWarningText(String warning) {
        TextView warningText = (TextView) findViewById(R.id.add_warning_text);
        warningText.setText(warning);
    }

    public void onBackButtonClicked(View view) {
        finish(); // This will navigate back to the previous activity
    }
}