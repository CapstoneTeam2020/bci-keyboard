package brainkeyboard;
import processing.core.*;
import neurosky.*;
import org.json.*;

import java.net.*; //for socket networks
import java.util.*;

public class Brainkeyboard extends PApplet{
    
    // create socket object for connection with headset
    ThinkGearSocket neuroSocket;
    int attention = 0;
    int meditation = 0;
    int blink_strength = 0;
    int blink = 0;
    int curr_index = 0; // to keep track of cursor
    
    // character set for virtual keyboard
    String[] letters = {"A","B","C","D","E","F","G","H","I","J","K","L"," "," ","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","-","-"};
    String input = " ";// string of selected characters
    String lockinput = " ";
    int t = 0;
    PFont font;
    PImage bg;//for keyboard Image
    int x_value = 30, y_value = 110;
    
    /* start connection*/
    public void settings(){
        size(550,300);
    }
    public void setup() {
        // size(500, 300);
        neuroSocket = new ThinkGearSocket(this);
        
        try{
            neuroSocket.start();
        }catch(ConnectException e){
            e.printStackTrace();
        }
        
        //fill(0);
        font = createFont("Courier",48);
        textFont(font);
        String path = "D:\\brainkeyboard\\src\\brainkeyboard\\1.jpg";
        bg = loadImage(path);
    }
    
    // this method keeps on running until program is closed.
    public void draw(){
        
        /*defining graphics for on screen keyboard*/
        background(120);
        fill(255, 255, 0);
        textSize(12);
        text("Attention: " + attention,100,10);
        strokeJoin(ROUND);
        stroke(50,100,150);
        strokeWeight(3);
        fill(255);
        rect(30,30,500,50);
        
        /*logic to select characters on eye blink*/
        if(blink_strength > 0){
            if(letters[curr_index] == "-"){
                //the last character entered should be deleted.
                input = input.substring(0,input.length()- 1);
            }else{
                input += letters[curr_index];
                lockinput = input;
                //lock input at that instance and display
                fill(0);
                textSize(18);
                text(input, 40, 48, 195, 30);
                textSize(48);
            }
        }
        
        fill(255, 255, 0);
        textSize(12);
        text("Blink: " + blink_strength, 10, 10);
        
        blink_strength = 0;
        fill(0);
        textSize(18);
        text(lockinput, 40, 48, 500, 300);
        textSize(48);
        
        /*----------*/
        image(bg, 30, 110); //load keyboard image
        noFill();
        stroke(255);
        rect(x_value, y_value, 50, 50); //moving box dimensions
        print("x:"+x_value +" " + "y:"+ y_value + " " + "k:"+ curr_index + "  " +"Blinkstrength:" + blink_strength);
        
        /*----*/
        if(attention > 80){
            t = 50;
            println("Attention: " + attention);
            attention = 0;
        }
        if(t == 50){
            if(x_value < 450){
                x_value += 50;
            }else{
                x_value = 30;
                if(y_value<200){
                    y_value+=50;
                }else{
                    y_value = 110;
                }
            }
            
            // letters have 30 elements
            // curr_index < 29
            if(curr_index < letters.length - 1){
                curr_index+=1;
            }else{
                // start from first character again;
                curr_index = 0;
            }
            t = 0;
        }
        t++;
    }
    
    public void blinkEvent(int blinkStrength){
        blink_strength = blinkStrength;
    }
    
    public void attentionEvent(int attentionLevel){
        attention = attentionLevel;
    }
    
    public void meditationEvent(int meditationLevel){
        meditation = meditationLevel;
    }
    
    public void stop(){
        neuroSocket.stop();
        super.stop();
    }
    
    public static void main(String[] args) {
        // if file is package then use packagename.Classname
        PApplet.main(new String[] { "--bgcolor=#ECE9D8", "brainkeyboard.Brainkeyboard" });
    }
}
