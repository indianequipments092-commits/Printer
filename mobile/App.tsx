import React, {useState} from "react";
import {SafeAreaView, View, Text, Pressable, StyleSheet, ScrollView} from "react-native";

const settings = ["DPI", "Brightness", "Contrast", "Color mode", "Paper size", "Auto-crop", "Deskew", "Duplex"];

export default function App() {
  const [scanning, setScanning] = useState(false);
  const [message, setMessage] = useState("Ready to discover a scanner");
  return <SafeAreaView style={styles.root}>
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.brand}>PrintBridge</Text>
      <Text style={styles.subtitle}>Print • Scan • Connect</Text>
      <View style={styles.card}>
        <Text style={styles.title}>Scanner</Text>
        <Text style={styles.message}>{message}</Text>
        <View style={styles.row}>
          <Pressable style={styles.button} onPress={() => {setScanning(true); setMessage("Discovering USB & wireless scanners…");}}>
            <Text style={styles.buttonText}>{scanning ? "Scanning…" : "Scan"}</Text>
          </Pressable>
          <Pressable style={styles.secondary} onPress={() => {setScanning(false); setMessage("Ready to discover a scanner");}}>
            <Text>Cancel</Text>
          </Pressable>
        </View>
      </View>
      <View style={styles.card}>
        <Text style={styles.title}>Scan settings</Text>
        {settings.map(item => <View style={styles.setting} key={item}><Text>{item}</Text><Text style={styles.value}>Default</Text></View>)}
      </View>
      <Text style={styles.note}>Hardware actions require the native USB/network adapters and a real device for final validation.</Text>
    </ScrollView>
  </SafeAreaView>;
}

const styles = StyleSheet.create({root:{flex:1},container:{padding:20,gap:16},brand:{fontSize:32,fontWeight:"800"},subtitle:{fontSize:16},card:{padding:18,borderWidth:1,borderRadius:18,gap:14},title:{fontSize:20,fontWeight:"700"},message:{opacity:.7},row:{flexDirection:"row",gap:10},button:{padding:14,borderRadius:12},secondary:{padding:14,borderWidth:1,borderRadius:12},buttonText:{fontWeight:"700"},setting:{flexDirection:"row",justifyContent:"space-between",paddingVertical:8},value:{opacity:.6},note:{fontSize:13,opacity:.65}}
);
