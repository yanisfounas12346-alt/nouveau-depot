package com.yanis.objectif30

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yanis.objectif30.ui.Objectif30Theme
import org.json.JSONObject
import org.json.JSONTokener

data class LeclercProduct(
    val id: String,
    val label: String,
    val price: Double,
    val pricePerUnit: String,
    val available: Boolean
)

class LeclercActivity : ComponentActivity() {
    companion object {
        const val EXTRA_INGREDIENTS = "wildsport_ingredients"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ingredients = intent.getStringArrayListExtra(EXTRA_INGREDIENTS) ?: arrayListOf()
        setContent {
            Objectif30Theme {
                WildCartLeclercScreen(
                    ingredients = ingredients,
                    onClose = { finish() }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WildCartLeclercScreen(
    ingredients: List<String>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var currentUrl by remember { mutableStateOf("") }
    var autoSelectIstresDone by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Drive préféré : Istres. Wildsport ouvre directement la page officielle d’Istres.") }
    var query by remember { mutableStateOf("") }
    var activeNeed by remember { mutableStateOf("") }
    var products by remember { mutableStateOf<List<LeclercProduct>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var cartMessage by remember { mutableStateOf("") }
    var showBrowser by remember { mutableStateOf(true) }
    var checklistRefresh by remember { mutableIntStateOf(0) }
    val checklist = remember { context.getSharedPreferences("wildsport_leclerc_checklist", Context.MODE_PRIVATE) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(false)
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            WebView.setWebContentsDebuggingEnabled(false)
            webChromeClient = WebChromeClient()

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    currentUrl = url.orEmpty()
                    CookieManager.getInstance().flush()

                    val istresLanding = currentUrl.contains("drive-istres.aspx", ignoreCase = true)
                    if (istresLanding && !autoSelectIstresDone) {
                        autoSelectIstresDone = true
                        view?.evaluateJavascript(
                            """
                            (() => {
                              const nodes = Array.from(document.querySelectorAll('a,button,input[type="button"],input[type="submit"]'));
                              const target = nodes.find(el => {
                                const txt = (el.innerText || el.value || el.textContent || '').trim().toLowerCase();
                                return txt.includes('choisir ce drive') || txt.includes('commencer mes courses');
                              });
                              if (target) { target.click(); return 'clicked'; }
                              return 'not-found';
                            })();
                            """.trimIndent(),
                            null
                        )
                    }

                    status = when {
                        currentUrl.contains("-courses.leclercdrive.fr") &&
                            Regex("magasin-\\d{6}-\\d{6}").containsMatchIn(currentUrl) ->
                            "✅ Drive Istres sélectionné. Appuie maintenant sur « Se connecter » dans la page E.Leclerc."
                        istresLanding ->
                            "Istres est chargé. Wildsport sélectionne automatiquement ce Drive."
                        currentUrl.contains("auth", ignoreCase = true) ||
                            currentUrl.contains("connexion", ignoreCase = true) ->
                            "Connexion E.Leclerc en cours…"
                        currentUrl.contains("leclerc", ignoreCase = true) ->
                            "Wildsport reste configuré sur le Drive d’Istres."
                        else ->
                            "Navigation E.Leclerc en cours."
                    }
                }
            }
            loadUrl("https://www.leclercdrive.fr/mobile/region-provence-alpes-cote-dazur/marseille/drive-istres.aspx")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    BackHandler {
        if (webView.canGoBack()) webView.goBack() else onClose()
    }

    fun decodeJsString(raw: String): String {
        return try {
            JSONTokener(raw).nextValue() as? String ?: raw
        } catch (_: Exception) {
            raw
        }
    }

    fun runSearch(searchText: String, needText: String = searchText) {
        if (searchText.isBlank() || busy) return
        activeNeed = needText
        busy = true
        cartMessage = ""
        val quoted = JSONObject.quote(searchText.trim())

        val script = """
            (async () => {
              const q = $quoted;
              const m = location.pathname.match(/magasin-(\d{6})-(\d{6})/);
              if (!m) return JSON.stringify({error:"Choisis d'abord ton Drive E.Leclerc dans la fenêtre."});
              const base = location.origin + "/magasin-" + m[1] + "-" + m[2];
              try {
                const r = await fetch(base + "/recherche.aspx?TexteRecherche=" + encodeURIComponent(q), {
                  credentials: "include",
                  headers: {"Accept":"text/html"}
                });
                const html = await r.text();
                if (!r.ok) return JSON.stringify({error:"Recherche HTTP " + r.status});
                if (!html.includes("lstProduitsLight")) {
                  return JSON.stringify({error:"Session Drive non reconnue ou challenge de sécurité. Recharge la page Drive puis réessaie."});
                }

                function smallest(raw, at) {
                  let start = -1, depth = 0;
                  for (let i = at; i >= 0; i--) {
                    const c = raw[i];
                    if (c === "}") depth++;
                    else if (c === "{") {
                      if (depth === 0) { start = i; break; }
                      depth--;
                    }
                  }
                  if (start < 0) return null;
                  depth = 0;
                  let inStr = null;
                  for (let j = start; j < raw.length; j++) {
                    const c = raw[j];
                    if (inStr) {
                      if (c === "\\") j++;
                      else if (c === inStr) inStr = null;
                      continue;
                    }
                    if (c === '"' || c === "'") inStr = c;
                    else if (c === "{") depth++;
                    else if (c === "}") {
                      depth--;
                      if (depth === 0) return raw.slice(start, j + 1);
                    }
                  }
                  return null;
                }

                function euro(v) {
                  if (typeof v === "number") return v;
                  if (!v) return 0;
                  const n = Number(String(v).replace(/[^\d,.-]/g, "").replace(",", "."));
                  return Number.isFinite(n) ? n : 0;
                }

                function decode(s) {
                  const t = document.createElement("textarea");
                  t.innerHTML = s || "";
                  return t.value;
                }

                const seen = new Set();
                const out = [];
                const re = /"iIdProduit"\s*:/g;
                let mm;
                while ((mm = re.exec(html)) && out.length < 40) {
                  const obj = smallest(html, mm.index);
                  if (!obj) continue;
                  try {
                    const p = JSON.parse(obj);
                    if (p.sType && p.sType !== "Produit") continue;
                    const id = String(p.iIdProduit);
                    if (seen.has(id)) continue;
                    seen.add(id);
                    const label = decode([p.sLibelleLigne1, p.sLibelleLigne2].filter(Boolean).join(" ").trim());
                    const price = Number(p.nrPVUnitaireTTC || 0) || euro(p.sPrixUnitaire);
                    out.push({
                      id,
                      label: label || ("Produit " + id),
                      price,
                      pricePerUnit: p.sPrixParUniteDeMesure || "",
                      available: Number(p.iQteDisponible || 0) > 0
                    });
                  } catch (_) {}
                }
                return JSON.stringify({storeId:m[1], products:out});
              } catch (e) {
                return JSON.stringify({error:String(e)});
              }
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { raw ->
            busy = false
            try {
                val payload = JSONObject(decodeJsString(raw))
                val error = payload.optString("error")
                if (error.isNotBlank()) {
                    status = error
                    products = emptyList()
                } else {
                    val array = payload.optJSONArray("products")
                    val parsed = mutableListOf<LeclercProduct>()
                    if (array != null) {
                        for (i in 0 until array.length()) {
                            val p = array.getJSONObject(i)
                            parsed += LeclercProduct(
                                id = p.optString("id"),
                                label = p.optString("label"),
                                price = p.optDouble("price", 0.0),
                                pricePerUnit = p.optString("pricePerUnit"),
                                available = p.optBoolean("available", false)
                            )
                        }
                    }
                    products = parsed
                    showBrowser = false
                    status = "Drive " + payload.optString("storeId") + " • " +
                        parsed.size + " résultats natifs dans Wildsport"
                }
            } catch (e: Exception) {
                status = "Réponse Leclerc illisible : " + (e.message ?: "erreur")
                products = emptyList()
            }
        }
    }

    fun addToCart(product: LeclercProduct, quantity: Int) {
        if (busy) return
        busy = true
        val productId = JSONObject.quote(product.id)
        val safeQty = quantity.coerceIn(1, 20)

        val script = """
            (async () => {
              const m = location.pathname.match(/magasin-(\d{6})-(\d{6})/);
              if (!m) return JSON.stringify({error:"Drive non détecté"});
              const base = location.origin + "/magasin-" + m[1] + "-" + m[2];
              const payload = {
                eTypeAction: 1,
                iIdProduit: $productId,
                iQuantite: $safeQty,
                sNoPointLivraison: m[1]
              };
              try {
                const r = await fetch(base + "/panier.aspx?op=1", {
                  method: "POST",
                  credentials: "include",
                  headers: {
                    "Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                    "X-Requested-With":"XMLHttpRequest",
                    "Accept":"application/json, text/javascript, */*; q=0.01"
                  },
                  body: "d=" + encodeURIComponent(JSON.stringify(payload))
                });
                const txt = await r.text();
                if (!r.ok) return JSON.stringify({error:"Panier HTTP " + r.status});
                const events = JSON.parse(txt);
                let total = "";
                let count = 0;
                for (const e of events) {
                  if (String(e.sIdUnique || "").startsWith("Panier")) {
                    const x = e.objElement || {};
                    total = x.sTotalAPayer || String(x.rTotalAPayer || "");
                    count = Number(x.iQuantitePanier || 0);
                  }
                }
                return JSON.stringify({ok:true,total,count});
              } catch (e) {
                return JSON.stringify({error:String(e)});
              }
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { raw ->
            busy = false
            try {
                val payload = JSONObject(decodeJsString(raw))
                val error = payload.optString("error")
                cartMessage = if (error.isNotBlank()) {
                    "❌ " + error
                } else {
                    "✅ " + product.label + " ajouté • panier " +
                        payload.optInt("count") + " article(s) • " +
                        payload.optString("total")
                }
            } catch (e: Exception) {
                cartMessage = "❌ Impossible de confirmer l'ajout."
            }
        }
    }

    val recommendation = remember(products, activeNeed) {
        if (activeNeed.isBlank()) null
        else BudgetProductMatcher.recommend(activeNeed, products)
    }

    if (showBrowser) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Surface(
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "E.Leclerc Drive • Istres",
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "Navigation plein écran",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showBrowser = false }) {
                        Text("Retour WildCart")
                    }
                }
            }

            AndroidView(
                factory = { webView },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "WILDSPORT",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "WildCart • E.Leclerc Drive",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                }
                TextButton(onClick = onClose) { Text("Fermer") }
            }
        }

        item {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Connexion privée • Istres", fontWeight = FontWeight.Bold)
                    Text(status)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "J’ai verrouillé WildCart sur E.Leclerc DRIVE Istres. Tes identifiants restent saisis uniquement dans la page officielle E.Leclerc.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            autoSelectIstresDone = false
                            webView.loadUrl("https://www.leclercdrive.fr/mobile/region-provence-alpes-cote-dazur/marseille/drive-istres.aspx")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("↻ Recharger mon Drive Istres")
                    }
                }
            }
        }

        item {
            Button(
                onClick = { showBrowser = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("↗ Ouvrir E.Leclerc en plein écran")
            }
            Text(
                "La page officielle s'ouvre seule, sans être imbriquée dans la liste Wildsport : tu peux faire défiler normalement tout le catalogue.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (ingredients.isNotEmpty()) {
            item {
                Text("🥦 Liste de courses de la semaine", fontWeight = FontWeight.Bold)
                Text(
                    "Coche ce que tu as déjà. Utilise Rechercher pour voir les vrais produits E.Leclerc sans perdre ta liste.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            items(ingredients.distinct()) { ingredient ->
                val key = ingredient.lowercase().trim()
                val checked = checklist.getBoolean(key, false)
                val normalized = ingredient
                    .replace(Regex("^\\s*\\d+(?:[.,]\\d+)?\\s*(?:g|kg|ml|cl|l)?\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^\\s*\\d+\\s*[×x]\\s*", RegexOption.IGNORE_CASE), "")
                    .trim()

                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { value ->
                                checklist.edit().putBoolean(key, value).apply()
                                checklistRefresh++
                            }
                        )
                        Text(
                            ingredient,
                            modifier = Modifier.weight(1f),
                            fontWeight = if (checked) FontWeight.Normal else FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = {
                                query = normalized.ifBlank { ingredient }
                                runSearch(query, ingredient)
                            }
                        ) {
                            Text("Rechercher")
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Chercher dans mon Drive") },
                placeholder = { Text("ex. blanc de poulet, riz, skyr…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Button(
                onClick = { runSearch(query, query) },
                enabled = !busy && query.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (busy) "Recherche…" else "Rechercher les vrais produits")
            }
        }

        if (cartMessage.isNotBlank()) {
            item {
                ElevatedCard {
                    Text(cartMessage, modifier = Modifier.padding(16.dp))
                }
            }
        }

        if (recommendation != null) {
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "💸 ACHAT RECOMMANDÉ",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            recommendation.product.label,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Besoin : " + activeNeed,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "À prendre : " + recommendation.packs + " paquet(s) • " +
                                "total ≈ " + "%.2f €".format(recommendation.totalCost)
                        )
                        val bought = BudgetProductMatcher.formatAmount(
                            recommendation.purchasedAmount,
                            recommendation.unit
                        )
                        val waste = BudgetProductMatcher.formatAmount(
                            recommendation.wasteAmount,
                            recommendation.unit
                        )
                        Text(
                            "Quantité achetée : " + bought + " • surplus estimé : " + waste,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (recommendation.product.pricePerUnit.isNotBlank()) {
                            Text(
                                recommendation.product.pricePerUnit,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                addToCart(
                                    recommendation.product,
                                    recommendation.packs
                                )
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Ajouter exactement " +
                                    recommendation.packs + " au panier"
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Wildsport choisit le produit qui couvre la quantité nécessaire au coût total le plus bas parmi les résultats disponibles, puis pénalise le gaspillage inutile.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        if (products.isNotEmpty()) {
            item {
                Text(
                    "Autres résultats E.Leclerc • " + products.size,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "Résultats affichés directement dans Wildsport pour éviter de naviguer dans la petite fenêtre du site.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        items(products) { product ->
            ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(product.label, fontWeight = FontWeight.Bold)
                    Text(
                        "%.2f €".format(product.price) +
                            if (product.pricePerUnit.isNotBlank()) " • " + product.pricePerUnit else ""
                    )
                    Text(if (product.available) "Disponible" else "Indisponible")
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { addToCart(product, 1) },
                        enabled = product.available && !busy
                    ) {
                        Text("Ajouter 1 au panier")
                    }
                }
            }
        }

        item {
            ElevatedCard {
                Column(Modifier.padding(16.dp)) {
                    Text("Beta personnelle", fontWeight = FontWeight.Bold)
                    Text(
                        "Cette connexion est non officielle et dépend du fonctionnement actuel d'E.Leclerc Drive. Elle peut casser si Leclerc modifie son site ou sa protection anti-bot. Wildsport confirme chaque ajout : aucun achat ni paiement automatique."
                    )
                }
            }
        }
    }
}
