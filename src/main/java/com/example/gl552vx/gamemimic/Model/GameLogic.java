package com.example.gl552vx.gamemimic.Model;



import java.util.Random;

public class GameLogic {
    private String[] mimic;

    private int[] counter;
    private Random rand;
    private String curMimic;

    public GameLogic(){
        this.rand=new Random();
        this.counter=new int[1];
        this.counter[0]=-1;

        this.mimic=new String[5];
        this.mimic[0]="MARAH";
        this.mimic[1]="TAKUT";
        this.mimic[2]="SENANG";
        this.mimic[3]="SEDIH";
        this.mimic[4]="KAGET";
    }


    public void generateMimic(){
        int number=-1;

        if(this.counter[0]==-1){
            number=rand.nextInt(5);
            this.curMimic=this.mimic[number];
        }
        else {
            if(this.counter[0]==number){
                while (this.counter[0]==number) {
                    number = rand.nextInt(5);
                }
            }

            this.curMimic=this.mimic[number];
        }

    }

    public String getCurMimic() {
        return curMimic;
    }

    public void setCurMimic(String curMimic) {
        this.curMimic = curMimic;
    }

    public void clear(){
        this.curMimic="";
        this.counter[0]=-1;
    }



}

