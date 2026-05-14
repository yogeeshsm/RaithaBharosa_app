package com.raitha.bharosa.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raitha.bharosa.data.Product
import com.raitha.bharosa.ui.theme.*
import com.raitha.bharosa.viewmodel.AppViewModel

private enum class StoreStep { GRID, DETAIL, CART, PAYMENT, SUCCESS }

@Composable
fun StoreScreen(viewModel: AppViewModel) {
    val lang by viewModel.lang.collectAsState()
    val cart by viewModel.cart.collectAsState()

    var step by remember { mutableStateOf(StoreStep.GRID) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    val orderId = remember { "RBH-${(100000..999999).random()}" }

    val totalItems = cart.sumOf { it.quantity }
    val totalPrice = cart.sumOf { it.product.price.toDouble() * it.quantity }

    when (step) {
        StoreStep.GRID -> ProductGrid(viewModel, totalItems, onSelect = { selectedProduct = it; step = StoreStep.DETAIL }, onCart = { step = StoreStep.CART })
        StoreStep.DETAIL -> ProductDetail(viewModel, selectedProduct!!, onBack = { step = StoreStep.GRID }, onCart = { step = StoreStep.CART })
        StoreStep.CART -> CartSheet(viewModel, cart, totalPrice, onBack = { step = StoreStep.GRID }, onCheckout = { step = StoreStep.PAYMENT })
        StoreStep.PAYMENT -> PaymentScreen(viewModel, totalPrice, onBack = { step = StoreStep.CART }, onPay = { viewModel.clearCart(); step = StoreStep.SUCCESS })
        StoreStep.SUCCESS -> OrderSuccess(viewModel, orderId, onDone = { step = StoreStep.GRID })
    }
}

@Composable
private fun ProductGrid(
    viewModel: AppViewModel, cartCount: Int,
    onSelect: (Product) -> Unit, onCart: () -> Unit
) {
    val lang by viewModel.lang.collectAsState()
    val products = com.raitha.bharosa.data.PRODUCTS

    Column(Modifier.fillMaxSize().background(Background)) {
        // Top bar
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(viewModel.t("store"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(viewModel.t("farmStore"), fontSize = 12.sp, color = Color.Gray)
            }
            BadgedBox(badge = {
                if (cartCount > 0) Badge(containerColor = BrandDanger) { Text("$cartCount") }
            }) {
                IconButton(onClick = onCart) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = BrandDeep)
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(products) { product ->
                ProductCard(product, viewModel, onClick = { onSelect(product) })
            }
            item { Spacer(Modifier.height(80.dp)) }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ProductCard(product: Product, viewModel: AppViewModel, onClick: () -> Unit) {
    val cart by viewModel.cart.collectAsState()
    val colors = listOf(
        Color(0xFF22C55E), Color(0xFF3B82F6), Color(0xFFF59E0B),
        Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF14B8A6),
        Color(0xFFF97316), Color(0xFF6366F1)
    )
    val colorIndex = (product.id.hashCode().and(0xFFFFFF)) % colors.size
    val bgColor = colors[colorIndex.coerceIn(0, colors.size - 1)]

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(120.dp).background(bgColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Eco, contentDescription = null, tint = bgColor, modifier = Modifier.size(48.dp))
                Surface(color = BrandDanger, shape = RoundedCornerShape(topStart = 0.dp, topEnd = 20.dp, bottomStart = 12.dp),
                    modifier = Modifier.align(Alignment.TopStart)) {
                    Text(product.badge, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(product.name, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(product.description, fontSize = 10.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("₹${product.price}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = BrandDeep)
                    Text("₹${product.originalPrice}", fontSize = 10.sp, color = Color.Gray, textDecoration = TextDecoration.LineThrough)
                }
                val cartItem = cart.find { it.id == product.id }
                if (cartItem != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                if (cartItem.quantity > 1) viewModel.updateQuantity(product.id, cartItem.quantity - 1)
                                else viewModel.removeFromCart(product.id)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (cartItem.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                                contentDescription = null,
                                tint = if (cartItem.quantity == 1) Color(0xFFDC2626) else BrandDeep,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            "${cartItem.quantity}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = BrandDeep
                        )
                        IconButton(
                            onClick = { viewModel.updateQuantity(product.id, cartItem.quantity + 1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = BrandDeep, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.addToCart(product) },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandDeep),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(viewModel.t("addToCart"), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductDetail(viewModel: AppViewModel, product: Product, onBack: () -> Unit, onCart: () -> Unit) {
    val colors = listOf(Color(0xFF22C55E), Color(0xFF3B82F6), Color(0xFFF59E0B), Color(0xFF8B5CF6))
    val bgColor = colors[(product.id.hashCode().and(0xFFFFFF)) % colors.size]

    Column(Modifier.fillMaxSize().background(Background)) {
        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(240.dp).background(bgColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Eco, contentDescription = null, tint = bgColor, modifier = Modifier.size(100.dp))
            }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(product.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text(product.category, fontSize = 11.sp, color = BrandDeep, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("₹${product.price}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = BrandDeep)
                        Text("₹${product.originalPrice}", fontSize = 12.sp, color = Color.Gray, textDecoration = TextDecoration.LineThrough)
                    }
                }
                Surface(color = Color(0xFFF0FDF4), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Description", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray, letterSpacing = 1.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(product.description, fontSize = 13.sp, color = OnBackground)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Icons.Default.Star, Icons.Default.Verified, Icons.Default.LocalShipping).zip(
                        listOf("4.8 Rating", "Certified", "Free Delivery")
                    ).forEach { (icon, label) ->
                        Surface(color = SurfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(icon, contentDescription = null, tint = BrandDeep, modifier = Modifier.size(20.dp))
                                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { viewModel.addToCart(product) },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandDeep),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(viewModel.t("addToCart"), fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { viewModel.addToCart(product); onCart() },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A5276)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(viewModel.t("buyNow"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CartSheet(
    viewModel: AppViewModel,
    cart: List<com.raitha.bharosa.data.CartItem>,
    totalPrice: Double,
    onBack: () -> Unit, onCheckout: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Background)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            Text(viewModel.t("cart"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(48.dp))
        }

        if (cart.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Text(viewModel.t("emptyCart"), fontSize = 14.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(cart) { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(BrandBg),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Eco, contentDescription = null, tint = BrandDeep, modifier = Modifier.size(32.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(item.product.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("₹${item.product.price} each", fontSize = 11.sp, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = { viewModel.updateQuantity(item.product.id, item.quantity - 1) },
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(SurfaceVariant)) {
                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                                Text("${item.quantity}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { viewModel.updateQuantity(item.product.id, item.quantity + 1) },
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(BrandDeep)) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Summary
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", fontSize = 12.sp, color = Color.Gray); Text("₹${"%.0f".format(totalPrice)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery", fontSize = 12.sp, color = Color.Gray); Text("FREE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    }
                    Divider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold); Text("₹${"%.0f".format(totalPrice)}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BrandDeep)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onCheckout,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandDeep),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(viewModel.t("checkout"), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PaymentScreen(viewModel: AppViewModel, total: Double, onBack: () -> Unit, onPay: () -> Unit) {
    var selectedPayment by remember { mutableStateOf("upi") }

    Column(Modifier.fillMaxSize().background(Background)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            Text(viewModel.t("payment"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(48.dp))
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Total
            Card(colors = CardDefaults.cardColors(containerColor = BrandDeep), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Amount", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    Text("₹${"%.0f".format(total)}", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("Raitha Bharosa Hub", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }

            Text("Payment Method", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray, letterSpacing = 1.sp)
            listOf(
                Triple("upi", "UPI / PhonePe", Icons.Default.PhoneAndroid),
                Triple("cod", "Cash on Delivery", Icons.Default.Money)
            ).forEach { (id, label, icon) ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { selectedPayment = id },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPayment == id) BrandBg else Surface
                    ),
                    shape = RoundedCornerShape(18.dp),
                    border = if (selectedPayment == id) BorderStroke(2.dp, BrandDeep) else null
                ) {
                    Row(Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, tint = if (selectedPayment == id) BrandDeep else Color.Gray, modifier = Modifier.size(24.dp))
                        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = if (selectedPayment == id) BrandDeep else OnBackground)
                        Spacer(Modifier.weight(1f))
                        if (selectedPayment == id) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandDeep, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Button(
            onClick = onPay,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandDeep),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Pay ₹${"%.0f".format(total)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun OrderSuccess(viewModel: AppViewModel, orderId: String, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Background), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(120.dp).clip(CircleShape).background(Color(0xFFDCFCE7)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(viewModel.t("orderPlaced"), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text("Your order has been placed!", fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = BrandBg), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Order ID", fontSize = 10.sp, color = Color.Gray, letterSpacing = 1.sp, fontWeight = FontWeight.ExtraBold)
                Text(orderId, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BrandDeep)
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.padding(horizontal = 48.dp).height(52.dp).fillMaxWidth(0.7f),
            colors = ButtonDefaults.buttonColors(containerColor = BrandDeep),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Continue Shopping", fontWeight = FontWeight.Bold) }
    }
}
