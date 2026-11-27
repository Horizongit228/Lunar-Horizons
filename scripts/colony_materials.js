(()=>{
const names=["energy_battery","helium_generator","oxygen_generator","electric_refinery","fuel_synthesizer","electric_refill","powered_lamp","power_terminal","advanced_assembler"];
const blocks=["power_cable","oxygen_pipe","oxygen_vent","habitat_hull","reinforced_hull","habitat_floor","industrial_grate","insulation_block","pressure_glass","airlock"];
const items=["control_circuit","power_cell","oxygen_filter","machine_frame"];
const hash=(x,y,s)=>{let n=Math.imul(x+41,374761393)^Math.imul(y+13,668265263)^Math.imul(s+37,2246822519);n=Math.imul(n^(n>>>13),1274126177);return ((n^(n>>>16))>>>0)/4294967295-.5;};
const C=(r,g,b,a=255)=>[r,g,b,a],shade=(c,v)=>C(c[0]+v,c[1]+v,c[2]+v,c[3]??255);
const box=(x,y,a,b,c,d)=>x>=a&&x<=c&&y>=b&&y<=d;
const accents=[[112,166,178],[177,132,73],[107,162,165],[168,117,72],[184,145,78],[85,157,174],[174,198,193],[116,163,148],[145,117,169]];
function tex(id,x,y){
 const on=id.endsWith("_on"),name=id.replace(/_on$/,""),i=names.indexOf(name),edge=Math.min(x,y,63-x,63-y),n=hash(x,y,i+21)*9;
 if(name==="pressure_glass"){if(edge<3)return shade([113,130,141],n);if(edge===3)return C(63,87,100);if((x+y)%39<2)return C(157,194,205,88);return C(91,132,150,42);}
 if(name==="insulation_block")return shade([151,153,149],n+((x+y)%4===0?-8:3));
 if(name==="habitat_floor"){if(edge<2)return C(46,53,60);if(x%16===0||y%16===0)return C(79,89,95);return shade([119,129,135],n+(x%8===2&&y%8===2?12:0));}
 if(name==="industrial_grate"){if(edge<4)return shade([132,143,150],n);return (x%8<3||y%8<3)?shade([117,127,135],n):shade([38,44,48],n);}
 if(name==="airlock"){if(edge<5)return shade([113,124,132],n);if(x===31||x===32)return C(34,40,44);if(y>48&&y<55)return shade((x>>3)%2?[163,125,61]:[47,53,58],n);if(box(x,y,26,24,29,39)||box(x,y,34,24,37,39))return C(151,168,173);return shade([92,104,113],n);}
 if(name==="power_cable"||name==="oxygen_pipe"){let color=name==="power_cable"?[158,113,62]:[100,159,172];if(edge<3)return shade([66,74,79],n);if(x%16<3||y%16<3)return shade([94,105,111],n);if(x>23&&x<40||y>23&&y<40)return shade(color,n+(x%7===0?10:0));return shade([53,61,67],n);}
 if(name==="oxygen_vent"){if(edge<5)return shade([128,142,148],n);if(x<12||x>51||y<12||y>51)return shade([75,91,99],n);return y%6<2?C(130,152,158):C(37,53,59);}
 if(i<0){if(edge<1)return C(46,54,61);if(edge===1)return C(172,183,187);if(edge===2)return C(81,92,100);if((x<8||x>55)&&(y<8||y>55)&&x%4===0&&y%4===0)return C(47,54,60);if(name==="reinforced_hull"&&(x%32===0||y%32===0||Math.abs(x-y)<2))return C(83,94,101);return shade(name==="reinforced_hull"?[109,119,128]:[153,159,160],n);}
 const accent=accents[i],base=i===0?[102,116,124]:[130,138,141];
 if(edge<2)return C(41,49,55);if(edge===2)return shade(base,35);if(edge===3)return shade(base,-28);
 if((x===6||x===57)&&(y===6||y===57))return C(45,53,58);
 if(y>=7&&y<=10)return shade(accent,on?14:-22);
 if(name==="powered_lamp"&&box(x,y,8,15,55,55)){if(x%12<2||y%12<2)return C(66,81,88);return shade(on?[215,226,217]:[103,118,121],n);}
 if(name==="energy_battery"&&box(x,y,10,15,53,45)){if(x<13||x>50||y<18||y>42)return C(46,55,62);return x%9<3?C(51,66,73):shade(on?[85,163,160]:[54,79,85],n);}
 if(box(x,y,9,15,54,31)){if(x<12||x>51||y<18||y>28)return C(41,53,59);if(y%5===0&&x>15&&x<46)return shade(on?accent:[67,81,87],n);return shade([29,45,51],n);}
 if(box(x,y,10,38,53,52)){if(name==="power_terminal")return (x%9<2||y%7<2)?C(50,63,70):shade([95,112,118],n);return y%4===0?C(45,53,59):shade([78,89,96],n);}
 if(y>=55&&y<=57&&x>=9&&x<=17)return on?shade(accent,38):C(55,63,68);
 return shade(base,n+Math.sin(y*.75)*2);
}
function item(id,x,y){let e=Math.min(x-9,y-9,54-x,54-y),n=hash(x,y,72)*8;
 if(id==="machine_frame"){if(e<0||box(x,y,19,19,44,44))return C(0,0,0,0);return shade(e<2?[55,66,74]:[151,162,169],n);}
 if(id==="control_circuit"){if(e<0)return C(0,0,0,0);if(e<2)return C(43,58,56);if(box(x,y,23,23,40,40))return shade([48,58,64],n);if(x%11<2||y%11<2)return shade([183,149,91],n);return shade([65,102,88],n);}
 if(id==="oxygen_filter"){if(e<0)return C(0,0,0,0);if(e<4)return shade([132,144,148],n);return y%6<2?C(61,74,82):shade([174,181,173],n);}
 if(id==="power_cell"){if(!box(x,y,18,8,45,55)&&!box(x,y,24,4,39,9))return C(0,0,0,0);if(x<21||x>42||y<13||y>51)return shade([107,122,130],n);if(y>19&&y<40)return shade([74,151,154],n);return shade([157,170,174],n);}return C(0,0,0,0);}
return {all:[...names.flatMap(id=>[id,id+"_on"]),...blocks].map(id=>({id,kind:"block"})).concat(items.map(id=>({id,kind:"item"}))),tex,item};
})()