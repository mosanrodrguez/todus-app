package cu.todus.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.app.ui.theme.*

data class Country(val flag: String, val code: String, val name: String)

@Composable
fun LoginScreen(onContinue: (String, String) -> Unit) {
    val countries = listOf(
        Country("🇨🇺", "+53", "Cuba"),
        Country("🇺🇸", "+1", "Estados Unidos"),
        Country("🇲🇽", "+52", "México"),
        Country("🇪🇸", "+34", "España"),
        Country("🇦🇷", "+54", "Argentina"),
        Country("🇨🇴", "+57", "Colombia"),
        Country("🇵🇪", "+51", "Perú"),
        Country("🇨🇱", "+56", "Chile"),
        Country("🇻🇪", "+58", "Venezuela"),
        Country("🇪🇨", "+593", "Ecuador"),
        Country("🇧🇷", "+55", "Brasil"),
        Country("🇩🇴", "+1", "República Dominicana"),
        Country("🇵🇦", "+507", "Panamá"),
        Country("🇨🇷", "+506", "Costa Rica"),
        Country("🇺🇾", "+598", "Uruguay"),
        Country("🇧🇴", "+591", "Bolivia"),
        Country("🇮🇹", "+39", "Italia"),
        Country("🇫🇷", "+33", "Francia"),
        Country("🇩🇪", "+49", "Alemania")
    )

    var selectedCountry by remember { mutableStateOf(countries[0]) }
    var phoneNumber by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var showCountryModal by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Ingresar número", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp))
            Text("Por favor, para continuar selecciona tu país e ingresa tu número de teléfono.", color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 340.dp).padding(bottom = 28.dp))

            Row(modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(modifier = Modifier.clickable { showCountryModal = true }, shape = RoundedCornerShape(14.dp), color = Input) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedCountry.flag, fontSize = 22.sp)
                        Text("  ${selectedCountry.code}  ▼", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { input ->
                        val digits = input.replace(Regex("[^0-9]"), "")
                        phoneNumber = if (digits.length > 4) digits.take(4) + " " + digits.drop(4).take(4) else digits
                        showError = false
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("5xxx xxxx", color = Color(0xFF5A5A5A)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, cursorColor = Red),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
            }
            if (showError) Text("Por favor ingresa un número válido", color = Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Button(
            onClick = {
                val digits = phoneNumber.replace(Regex("[^0-9]"), "")
                if (digits.length >= 6) onContinue(selectedCountry.code.replace("+", "") + digits, java.util.UUID.randomUUID().toString())
                else showError = true
            },
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp).height(30.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Red),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Continuar", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
    }

    if (showCountryModal) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0A0A)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().background(Surface).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showCountryModal = false }) { Text("←", color = TextWhite, fontSize = 20.sp) }
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.weight(1f), placeholder = { Text("Buscar país...", color = TextMuted) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent), shape = RoundedCornerShape(12.dp), singleLine = true)
                }
                LazyColumn {
                    items(countries.filter { it.name.lowercase().contains(searchQuery.lowercase()) }) { country ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { selectedCountry = country; showCountryModal = false; searchQuery = "" }.padding(14.dp, 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(country.flag, fontSize = 26.sp, modifier = Modifier.width(36.dp))
                            Text(country.name, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text(country.code, color = TextMuted, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}
