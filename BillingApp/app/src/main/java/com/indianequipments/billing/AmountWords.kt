package com.indianequipments.billing

object AmountWords {
    private val ones=arrayOf("zero","one","two","three","four","five","six","seven","eight","nine","ten","eleven","twelve","thirteen","fourteen","fifteen","sixteen","seventeen","eighteen","nineteen")
    private val tens=arrayOf("","","twenty","thirty","forty","fifty","sixty","seventy","eighty","ninety")
    private fun under1000(n:Int):String{var x=n;val p=mutableListOf<String>();if(x>=100){p+=ones[x/100];p+="hundred";x%=100};if(x>0){if(p.isNotEmpty())p+="and";if(x<20)p+=ones[x]else{p+=tens[x/10];if(x%10>0)p+=ones[x%10]}};return p.joinToString(" ")}
    fun inr(value:Double):String{val rupees=value.toLong();val paise=((value*100).toLong()%100).toInt();if(rupees==0L&&paise==0)return "Rupees Zero Only";var n=rupees;val parts=mutableListOf<String>();val crore=n/10000000;n%=10000000;val lakh=n/100000;n%=100000;val thousand=n/1000;n%=1000;if(crore>0)parts+="${under1000(crore.toInt())} crore";if(lakh>0)parts+="${under1000(lakh.toInt())} lakh";if(thousand>0)parts+="${under1000(thousand.toInt())} thousand";if(n>0)parts+=under1000(n.toInt());var out="Rupees "+parts.joinToString(" ");if(paise>0)out+=" and ${under1000(paise)} paise";return out+" Only"}
}
