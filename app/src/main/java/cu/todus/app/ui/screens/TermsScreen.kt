package cu.todus.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.app.ui.theme.Red
import cu.todus.app.ui.theme.TextWhite

@Composable
fun TermsScreen(onAccept: () -> Unit) {
    val context = LocalContext.current

    val termsText = buildAnnotatedString {
        withStyle(SpanStyle(color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Normal)) {
            append("Al continuar, aceptas nuestros ")
        }
        pushStringAnnotation("terms", "https://todus.cu/terms.html")
        withStyle(SpanStyle(color = Red, fontSize = 14.sp, fontWeight = FontWeight.Medium)) {
            append("Términos")
        }
        pop()
        withStyle(SpanStyle(color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Normal)) {
            append(" y ")
        }
        pushStringAnnotation("conditions", "https://todus.cu/terms.html")
        withStyle(SpanStyle(color = Red, fontSize = 14.sp, fontWeight = FontWeight.Medium)) {
            append("Condiciones")
        }
        pop()
        withStyle(SpanStyle(color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Normal)) {
            append(" de uso.")
        }
    }

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
            Text(
                text = "toDus",
                color = Red,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Text(
                text = "Bienvenidos a toDus",
                color = Red,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            ClickableText(
                text = termsText,
                style = TextStyle(textAlign = TextAlign.Center, lineHeight = 22.sp),
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .padding(bottom = 36.dp),
                onClick = { offset ->
                    termsText.getStringAnnotations("terms", offset, offset).firstOrNull()?.let {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
                    }
                    termsText.getStringAnnotations("conditions", offset, offset).firstOrNull()?.let {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
                    }
                }
            )
        }

        Button(
            onClick = onAccept,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                .height(30.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Red),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Aceptar y Continuar",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
