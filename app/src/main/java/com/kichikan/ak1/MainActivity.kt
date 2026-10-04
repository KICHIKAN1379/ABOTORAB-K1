package com.kichikan.ak1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class Member(val id:Int,val name:String,val level:Int,val positive:Int,val negative:Int) {
    val total:Int get() = positive - negative
}
class AppState {
    val members = mutableStateListOf(
        Member(1,"محمدعلی",3,42,2),
        Member(2,"علی",2,31,1),
        Member(3,"رضا",2,27,3),
        Member(4,"محمد",1,18,0)
    )
    fun addPoints(id:Int, amount:Int, positive:Boolean) {
        val i=members.indexOfFirst { it.id==id }
        if(i<0) return
        val old=members[i]
        members[i]=if(positive) old.copy(positive=old.positive+amount)
        else old.copy(negative=old.negative+amount)
    }
}
private val AK1Colors=darkColorScheme(
    primary=Color(0xFFFFD700),
    secondary=Color(0xFF38EF7D),
    background=Color(0xFF16213E),
    surface=Color(0xFF101827)
)
class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContent{MaterialTheme(colorScheme=AK1Colors){AK1App()}}
    }
}
private enum class Tab{HOME,MEMBERS,RANKING,SETTINGS}
@Composable fun AK1App(){
    val state=remember{AppState()}
    var tab by remember{mutableStateOf(Tab.HOME)}
    Scaffold(bottomBar={
        NavigationBar{
            NavigationBarItem(tab==Tab.HOME,{tab=Tab.HOME},{Icon(Icons.Default.Home,null)},{Text("خانه")})
            NavigationBarItem(tab==Tab.MEMBERS,{tab=Tab.MEMBERS},{Icon(Icons.Default.Groups,null)},{Text("اعضا")})
            NavigationBarItem(tab==Tab.RANKING,{tab=Tab.RANKING},{Icon(Icons.Default.EmojiEvents,null)},{Text("رتبه")})
            NavigationBarItem(tab==Tab.SETTINGS,{tab=Tab.SETTINGS},{Icon(Icons.Default.Settings,null)},{Text("تنظیمات")})
        }
    }){padding->
        when(tab){
            Tab.HOME->Dashboard(state,padding)
            Tab.MEMBERS->Members(state,padding)
            Tab.RANKING->Ranking(state,padding)
            Tab.SETTINGS->Settings(padding)
        }
    }
}
@Composable private fun Dashboard(state:AppState,padding:PaddingValues){
    var dialog by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        Text("ابوتراب ۳",style=MaterialTheme.typography.headlineMedium)
        Text("مربی ابوتراب",color=MaterialTheme.colorScheme.primary)
        Card(Modifier.fillMaxWidth()){
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text("خلاصه امروز",style=MaterialTheme.typography.titleLarge)
                Text("اعضا: "+state.members.size)
                Text("امتیاز مثبت: "+state.members.sumOf{it.positive})
                Text("امتیاز منفی: "+state.members.sumOf{it.negative})
            }
        }
        Button({dialog=true},Modifier.fillMaxWidth()){
            Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text("ثبت امتیاز")
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Card(Modifier.weight(1f)){Box(Modifier.padding(18.dp),contentAlignment=Alignment.Center){Text("حضور و غیاب")}}
            Card(Modifier.weight(1f)){Box(Modifier.padding(18.dp),contentAlignment=Alignment.Center){Text("رتبه‌بندی")}}
        }
    }
    if(dialog)PointDialog(state){dialog=false}
}
@Composable private fun Members(state:AppState,padding:PaddingValues){
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)){
        Text("اعضای گروه",style=MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
            items(state.members,key={it.id}){m->
                Card(Modifier.fillMaxWidth()){
                    Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween){
                        Column{Text(m.name,style=MaterialTheme.typography.titleLarge);Text("سطح "+m.level)}
                        Column(horizontalAlignment=Alignment.End){
                            Text("+"+m.positive,color=MaterialTheme.colorScheme.primary)
                            Text("-"+m.negative,color=MaterialTheme.colorScheme.error)
                            Text("نهایی: "+m.total)
                        }
                    }
                }
            }
        }
    }
}
@Composable private fun Ranking(state:AppState,padding:PaddingValues){
    val ranked=state.members.sortedByDescending{it.total}
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)){
        Text("رتبه‌بندی",style=MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
            itemsIndexed(ranked,key={_,m->m.id}){index,m->
                Card(Modifier.fillMaxWidth()){
                    Row(Modifier.fillMaxWidth().padding(18.dp),horizontalArrangement=Arrangement.SpaceBetween){
                        Text("#"+(index+1)+"  "+m.name)
                        Text(m.total.toString()+" امتیاز")
                    }
                }
            }
        }
    }
}
@Composable private fun Settings(padding:PaddingValues){
    var notifications by remember{mutableStateOf(true)}
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        Text("تنظیمات",style=MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth()){
            Row(Modifier.fillMaxWidth().padding(18.dp),horizontalArrangement=Arrangement.SpaceBetween){
                Column{Text("یادآوری‌ها");Text("اعلان‌های تربیتی و رویدادها")}
                Switch(notifications,{notifications=it})
            }
        }
        Text("مربی ابوتراب — نسخه 1.0")
    }
}
@Composable private fun PointDialog(state:AppState,onDismiss:()->Unit){
    var selected by remember{mutableStateOf(state.members.firstOrNull()?.id?:0)}
    var positive by remember{mutableStateOf(true)}
    var amount by remember{mutableStateOf("1")}
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("ثبت امتیاز")},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                state.members.forEach{m->
                    FilterChip(selected==m.id,{selected=m.id},label={Text(m.name)})
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    FilterChip(positive,{positive=true},label={Text("مثبت")})
                    FilterChip(!positive,{positive=false},label={Text("منفی")})
                }
                OutlinedTextField(amount,{amount=it.filter(Char::isDigit)},label={Text("مقدار")},singleLine=true)
            }
        },
        confirmButton={
            TextButton({
                val value=amount.toIntOrNull()?:1
                if(value>0&&selected!=0)state.addPoints(selected,value,positive)
                onDismiss()
            }){Text("ثبت")}
        },
        dismissButton={TextButton(onDismiss){Text("انصراف")}}
    )
}
