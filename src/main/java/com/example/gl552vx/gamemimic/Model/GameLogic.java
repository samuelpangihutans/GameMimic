package com.example.gl552vx.gamemimic.Model;


import java.util.ArrayList;
import java.util.Random;

public class GameLogic {
    private String[] mimic;
    private ArrayList<Integer> counter;
    private Random rand;

    public GameLogic(){
        this.rand=new Random();
        this.counter=new ArrayList<>();
        this.mimic=new String[5];
        this.mimic[0]="MARAH";
        this.mimic[1]="TAKUT";
        this.mimic[2]="SENANG";
        this.mimic[3]="SEDIH";
        this.mimic[4]="KAGET";
    }

    public String generateMimic(){
        int number=-1;
        boolean flag=false;
        boolean finish=false;
        if(counter.size()==0){
            number=rand.nextInt(5);
            finish=true;
            return this.mimic[number];
        }
        else {
            while (finish == false) {
                number = rand.nextInt(5);
                for (int i = 0; i < counter.size(); i++) {
                    if (number == counter.get(i)) {
                        flag = true;
                    }
                }
                if (flag == false) {
                    finish=true;
                    return this.mimic[number];

                } else {
                    finish=false;
                }

            }
        }
        return null;

    }

    public void clear(){
        counter.clear();
    }



}
