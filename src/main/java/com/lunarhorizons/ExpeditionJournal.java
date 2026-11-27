package com.lunarhorizons;
import java.util.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/** Immutable, localized recovered records, with independently generated fictional telemetry. */
final class ExpeditionJournal {
    private static final String PREFIX="journal.lunar_horizons.";
    private static Text tr(String key,Object... values){return Text.translatable(PREFIX+key,values);}
    private static void page(List<RawFilteredPair<Text>> pages,String key,Object... values){pages.add(RawFilteredPair.<Text>of(tr(key,values)));}
    static ItemStack create(int type,Random r,BlockPos pos){
        List<RawFilteredPair<Text>> pages=new ArrayList<>();
        String id=(type>=9?"EXP":type==2?"SAT":type==6?"LND":type==7?"RLY":type<2?"OUT":"LAB")+"-"+(100+r.nextInt(900));
        int power=5+r.nextInt(56),signal=2+r.nextInt(39),damage=30+r.nextInt(61);
        String time=String.format(Locale.ROOT,"%02d:%02d:%02d",r.nextInt(24),r.nextInt(60),r.nextInt(60));
        page(pages,"record",tr(type+".title"),id,time);
        if(type==2){
            page(pages,"sat_status",tr("mission."+r.nextInt(4)),power,signal);
            page(pages,"sat_damage",damage,100-damage,r.nextInt(61),tr("orientation."+r.nextInt(3)));
            page(pages,"sat_impact",65+r.nextInt(176),-170+r.nextInt(76),tr("fault."+r.nextInt(4)));
            page(pages,"sat_repair");
        }else{
            page(pages,"site_status",power,damage,2+r.nextInt(19),tr(type+".line1"));
            page(pages,"site_note",tr(type+".line2"));
            if(type>=3&&type<=5)page(pages,"lab_samples",3+r.nextInt(29),1+r.nextInt(5),tr("sample."+r.nextInt(3)));
            else if(type==7)page(pages,"relay_data",signal,1+r.nextInt(12));
            else if(type==6)page(pages,"lander_data",2+r.nextInt(12),power);
            else if(type>=9)page(pages,"site_record",1+r.nextInt(80),signal,1+r.nextInt(12));
            else page(pages,"outpost_reserve",12+r.nextInt(89),1+r.nextInt(9));
        }
        if(pos==null)page(pages,"unknown_location");else page(pages,"location",pos.getX(),pos.getY(),pos.getZ());
        ItemStack book=new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponentTypes.CUSTOM_NAME,tr(type+".title"));
        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT,new WrittenBookContentComponent(RawFilteredPair.of(id),"Lunar Horizons",0,pages,true));
        return book;
    }
    static void upgradeInventory(ServerPlayerEntity p){
        var inv=p.getInventory();
        for(int i=0;i<inv.size();i++){
            ItemStack old=inv.getStack(i);
            if(!old.isOf(Items.BOOK))continue;
            Text title=old.get(DataComponentTypes.CUSTOM_NAME);
            if(title==null||!(title.getContent() instanceof TranslatableTextContent tc))continue;
            for(int type=0;type<6;type++)if(tc.getKey().equals(PREFIX+type+".title")){
                ItemStack converted=create(type,p.getRandom(),null);
                int remaining=old.getCount()-1;inv.setStack(i,converted);
                if(remaining>0){ItemStack rest=old.copy();rest.setCount(remaining);if(!inv.insertStack(rest)&&!rest.isEmpty())p.dropItem(rest,false);}
                inv.markDirty();break;
            }
        }
    }
}
