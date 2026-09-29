package com.yanis.objectif30

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
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
    var status by remember { mutableStateOf("Étape 1 : sélectionne ton Drive. Étape 2 : connecte-toi à ton compte E.Leclerc.") }
    var query by remember { mutableStateOf("") }
    var products by remember { mutableStateOf<List<LeclercProduct>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var cartMessage by remember { mutableStateOf("") }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            WebView.setWebContentsDebuggingEnabled(false)

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    currentUrl = url.orEmpty()
                    status = when {
                        currentUrl.contains("-courses.leclercdrive.fr") &&
                            Regex("magasin-\\d{6}-\\d{6}").containsMatchIn(currentUrl) ->
                            "✅ Drive détecté. Tu peux maintenant te connecter à ton compte E.Leclerc dans cette page."
                        currentUrl.contains("leclerc", ignoreCase = true) ->
                            "Choisis d’abord ton Drive (ville/code postal), puis appuie sur Se connecter."
                        else ->
                            "Ouvre E.Leclerc Drive dans cette fenêtre."
                    }
                }
            }
            loadUrl("https://www.leclercdrive.fr/mobile/")
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

    fun runSearch(searchText: String) {
        if (searchText.isBlank() || busy) return
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
                while ((mm = re.exec(html)) && out.length < 18) {
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
                    status = "Drive " + payload.optString("storeId") + " • " +
                        parsed.size + " résultats"
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
                    Text("Connexion privée", fontWeight = FontWeight.Bold)
                    Text(status)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Wildsport ne te demande pas ton mot de passe. Tu te connectes directement dans la page officielle E.Leclerc ci-dessous.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                AndroidView(
                    factory = { webView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp)
                )
            }
        }

        if (ingredients.isNotEmpty()) {
            item {
                Text("🥦 Ingrédients de tes menus", fontWeight = FontWeight.Bold)
                Text(
                    "Appuie sur un ingrédient pour le rechercher dans ton Drive.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            items(ingredients.distinct().take(18)) { ingredient ->
                val normalized = ingredient
                    .replace(Regex("^\\s*\\d+(?:[.,]\\d+)?\\s*(?:g|kg|ml|l)?\\s*", RegexOption.IGNORE_CASE), "")
                    .trim()
                OutlinedButton(
                    onClick = {
                        query = normalized.ifBlank { ingredient }
                        runSearch(query)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔎 " + ingredient)
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
                onClick = { runSearch(query) },
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
