/* Deterministic hard-SF material and sprite generator. No external dependencies.
 * Evaluates to { generate, decode, png }; outputs standard RGBA PNG base64.
 * All coordinates in item designs use a 32-unit canvas, rendered at 64px. */
(()=>{
const C=(r,g,b,a=255)=>[r,g,b,a].map(v=>Math.max(0,Math.min(255,Math.round(v))));
const T=[0,0,0,0],mix=(a,b,t)=>a.map((v,i)=>v+(b[i]-v)*t),shade=(a,v)=>C(a[0]+v,a[1]+v,a[2]+v,a[3]);
const hash=(x,y,s=0)=>{let n=Math.imul(x+41,374761393)^Math.imul(y+13,668265263)^Math.imul(s+37,2246822519);n=Math.imul(n^(n>>>13),1274126177);return ((n^(n>>>16))>>>0)/4294967295;};
const smooth=t=>t*t*(3-2*t);
function noise(x,y,scale,seed=0,period=64){let n=period/scale,xx=x/scale,yy=y/scale,i=Math.floor(xx),j=Math.floor(yy),u=smooth(xx-i),v=smooth(yy-j),mod=a=>((a%n)+n)%n;let a=hash(mod(i),mod(j),seed),b=hash(mod(i+1),mod(j),seed),c=hash(mod(i),mod(j+1),seed),d=hash(mod(i+1),mod(j+1),seed);return (a+(b-a)*u)*(1-v)+(c+(d-c)*u)*v;}
const fb=(x,y,s=0)=>.45*noise(x,y,16,s)+.27*noise(x,y,8,s+1)+.18*noise(x,y,4,s+2)+.10*noise(x,y,2,s+3);
const grain=(x,y,s=0)=>hash(x,y,s)-.5;
const alpha="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
function encode(out){let s="";for(let i=0;i<out.length;i+=3){let v=(out[i]<<16)|((out[i+1]??0)<<8)|(out[i+2]??0);s+=alpha[v>>>18]+alpha[v>>>12&63]+(i+1<out.length?alpha[v>>>6&63]:"=")+(i+2<out.length?alpha[v&63]:"=");}return s;}
function b64(s){let out=[],v=0,b=0;for(const c of s){if(c==="=")break;const n=alpha.indexOf(c);if(n<0)continue;v=(v<<6)|n;b+=6;if(b>=8){b-=8;out.push(v>>>b&255)}}return out;}
const be=n=>[n>>>24&255,n>>>16&255,n>>>8&255,n&255],u32=(d,i)=>(d[i]<<24|d[i+1]<<16|d[i+2]<<8|d[i+3])>>>0;
function png(w,h,fn){const raw=[];for(let y=0;y<h;y++){raw.push(0);for(let x=0;x<w;x++)raw.push(...C(...fn(x,y)));}const chunk=(name,body)=>{const a=[...name].map(x=>x.charCodeAt(0)).concat(body);let crc=-1;for(const b of a){crc^=b;for(let k=0;k<8;k++)crc=(crc>>>1)^((crc&1)?0xedb88320:0);}return [...be(body.length),...a,...be((crc^-1)>>>0)];};let z=[120,1],a=1,b=0;for(let off=0;off<raw.length;off+=65535){const len=Math.min(65535,raw.length-off);z.push(off+len===raw.length?1:0,len&255,len>>>8,(~len)&255,(~len)>>>8&255);for(let i=off;i<off+len;i++){const v=raw[i];z.push(v);a=(a+v)%65521;b=(b+a)%65521;}}z.push(...be(((b<<16)|a)>>>0));return encode([137,80,78,71,13,10,26,10,...chunk("IHDR",[...be(w),...be(h),8,6,0,0,0]),...chunk("IDAT",z),...chunk("IEND",[])]);}
function decode(s){const d=b64(s),w=u32(d,16),h=u32(d,20),z=[];for(let i=8;i<d.length;){const n=u32(d,i);if(String.fromCharCode(...d.slice(i+4,i+8))==="IDAT")for(let j=i+8;j<i+8+n;j++)z.push(d[j]);i+=n+12;}let raw=[],i=2;while(i<z.length-4){let flag=z[i++],len=z[i]|z[i+1]<<8;i+=4;if((flag&6)!==0)throw Error("expected stored PNG");for(let j=0;j<len;j++)raw.push(z[i++]);if(flag&1)break;}if(raw.length!==h*(w*4+1))throw Error("bad scanlines");return {w,h,pixel:(x,y)=>raw.slice(y*(w*4+1)+1+x*4,y*(w*4+1)+5+x*4)};}
const poly=(x,y,p)=>{let inside=false;for(let i=0,j=p.length-1;i<p.length;j=i++){const [a,b]=p[i],[c,d]=p[j];if((b>y)!==(d>y)&&x<(c-a)*(y-b)/(d-b)+a)inside=!inside;}return inside;};
function dist(x,y,a,b,c,d){let t=Math.max(0,Math.min(1,((x-a)*(c-a)+(y-b)*(d-b))/((c-a)**2+(d-b)**2||1)));return Math.hypot(x-a-t*(c-a),y-b-t*(d-b));}
const rect=(x,y,a,b,c,d)=>x>=a&&x<=c&&y>=b&&y<=d;
const rockColors={regolith:[111,109,105],moon_rock:[92,94,96],highland_rock:[125,125,124],dark_regolith:[65,64,69],frosted_regolith:[129,139,145],crystal_stone:[65,86,91],meteorite_rock:[55,48,46]};
function rock(x,y,id){let color=rockColors[id]||rockColors.moon_rock,seed=Object.keys(rockColors).indexOf(id)+13;let broad=fb(x,y,seed),fine=grain(x,y,seed),vein=noise(x,y,8,seed+21)+noise(x,y,4,seed+22)*.22;let crack=id==="regolith"||id==="frosted_regolith"?0:Math.max(0,1-Math.abs(vein-.57)*42)*-18;let grit=hash(x,y,seed+2)>.965?15:0;return shade(color,(broad-.5)*68+fine*17+crack+grit);}
const oreColors={crystal_ore:[[31,66,73],[80,139,146],[158,208,211]],dark_crystal_ore:[[44,32,55],[96,74,119],[162,138,182]],titanium_ore:[[68,78,85],[137,146,153],[201,208,211]],helium_ore:[[53,76,58],[112,145,111],[181,204,163]],meteorite_rock:[[66,42,33],[121,79,51],[181,124,75]]};
function ore(x,y,id){const cols=oreColors[id],base=rock(x,y,id==="meteorite_rock"?"meteorite_rock":id==="dark_crystal_ore"?"dark_regolith":"moon_rock");for(let i=0;i<10;i++){let cx=hash(i,1,77)*64,cy=hash(i,2,77)*64,r=2.4+hash(i,3,77)*4.4;let dx=((x-cx+96)%64)-32,dy=((y-cy+96)%64)-32;let angle=hash(i,4,77)*6.28,xx=dx*Math.cos(angle)+dy*Math.sin(angle),yy=-dx*Math.sin(angle)+dy*Math.cos(angle);let q=Math.abs(xx)*.7+Math.abs(yy)*1.25+(noise(x,y,2,i)-.5)*2;if(q<r){let edge=q/r;if(edge>.79)return shade(cols[0],grain(x,y)*12);return shade(mix(cols[1],cols[2],Math.max(0,Math.min(1,.4-xx/r*.4-yy/r*.45))),(fb(x,y,i)-.5)*22+grain(x,y)*12);}}return base;}
function metal(x,y,base=[142,151,154]){return shade(base, (noise(x,y,16,3)-.5)*19+grain(x,y)*10+Math.sin(y*1.6)*2.5);}
function panel(x,y,id){let edge=Math.min(x,y,63-x,63-y),base=[126,135,140];if(id==="rocket_body"||id==="rocket_nose")base=[191,192,185];if(id==="rocket_engine"||id==="launch_pad")base=[69,76,80];if(id==="oxygen_station"||id==="oxygen_station_top"||id==="survey_beacon")base=[104,116,124];if(edge<1)return C(42,48,50);if(edge===1)return shade(base,32);if(edge===2)return shade(base,-26);if((x===5||x===58)&&(y===5||y===58))return C(48,52,55);if((x===6||x===57)&&(y===6||y===57))return C(186,187,178);
 if(id==="launch_pad"){if(edge<8&&((x+y)>>3)%2===0)return shade([163,126,60],grain(x,y)*17);if(x%8<2&&y%8<2)return C(37,43,47);return metal(x,y,base);}
 if(id==="rocket_engine"){if(y%12<2)return C(40,44,47);return metal(x,y,shade(base,Math.sin(x*.45)*18));}
 if(id==="rocket_body"||id==="rocket_nose"){if(y===20||y===43)return C(88,94,96);if(y===21||y===44)return C(215,217,208);if(id==="rocket_nose"&&y>46)return metal(x,y,[111,54,46]);return metal(x,y,base);}
 if(id==="lunar_lantern"){if(edge<7)return metal(x,y,[79,92,99]);if(edge===7||x%14===0)return C(62,107,116);return shade([179,211,211],(fb(x,y,3)-.5)*29);}
 if(id==="oxygen_station"||id==="survey_beacon"){if(rect(x,y,9,10,54,31)){if(x<12||x>51||y<13||y>28)return C(38,49,56);return y%5===0?C(120,174,166):shade([28,57,63],grain(x,y)*9);}if(rect(x,y,10,39,53,53))return y%4===0?C(40,48,53):metal(x,y,[77,87,92]);}
 if(id==="oxygen_station_top"){if(Math.hypot(x-32,y-32)<19){let r=Math.hypot(x-32,y-32);return r>16?C(56,68,72):r>12?C(132,157,160):C(41,63,69);}}
 return metal(x,y,base);}
function block(id,x,y){
 if(id in oreColors)return ore(x,y,id);
 if(id in rockColors)return rock(x,y,id);
 if(id==="lunar_ice"){let f=fb(x,y,6),v=noise(x,y,8,14)+noise(x,y,4,15)*.25,cr=Math.abs(v-.54)<.025;return shade(mix([72,115,137],[174,198,205],f),grain(x,y)*8+(cr?34:0));}
 if(id==="moon_bricks"){let row=Math.floor(y/16),xx=(x+(row%2)*16)%32,yy=y%16;if(yy<1||xx<1)return C(39,44,49);if(yy===1||xx===1)return C(131,137,142);if(yy===15||xx===31)return C(62,67,73);return shade(rock(x,y,"moon_rock"),5+(hash(Math.floor((x+(row%2)*16)/32),row)-.5)*15);}
 if(id==="crystal_cluster"||id==="lunar_stems"){let base=id==="crystal_cluster"?[78,138,146]:[67,88,103];let band=x%16;return shade(base,band<2?-30:band<6?28:band>13?-17:0+(fb(x,y,4)-.5)*25+grain(x,y)*7);}
 if(id==="rocket_window"){let e=Math.min(x,y,63-x,63-y);return e<5?metal(x,y,[92,108,119]):shade([37,74,90],(x+y<42?35:0)+grain(x,y)*4);}
 if(id==="flag_metal")return metal(x,y,[138,147,151]);
 if(id.endsWith("_flag")){let c={expedition_flag:[200,201,193],red_flag:[129,54,51],blue_flag:[48,71,98],orange_flag:[166,104,54]}[id];if(id==="expedition_flag"){let dx=x-32,dy=y-31,r=Math.hypot(dx,dy);if(r<21)c=[36,55,72];if(r>=19&&r<21)c=[116,137,148];if(r<16&&Math.hypot(dx-6,dy+5)>12)c=[216,218,206];if(r<19&&dy>9&&dy<12)c=[160,103,63];if((Math.abs(x-41)<1&&Math.abs(y-21)<4)||(Math.abs(y-21)<1&&Math.abs(x-41)<4))c=[231,226,210];}else if(y>=50&&y<=53)c=[175,179,174];let edge=Math.min(x,y,63-x,63-y);return shade(c,(fb(x,y,8)-.5)*14+grain(x,y)*8+Math.sin(x*.34)*5+(edge<2?-22:edge===3&&(x+y)%5<3?18:0));}
 return panel(x,y,id);
}
function shape(points,col,kind="metal"){return {test:(x,y)=>poly(x,y,points),col,kind};}
function line(a,b,c,d,r,col,kind="metal"){return {test:(x,y)=>dist(x,y,a,b,c,d)<=r,col,kind};}
function box(a,b,c,d,col,kind="metal"){return {test:(x,y)=>rect(x,y,a,b,c,d),col,kind};}
const steel=[151,163,169],dark=[53,63,70],pale=[195,200,196],teal=[81,136,146],violet=[111,87,137],amber=[155,104,58],rubber=[67,61,55];
function layers(id){let s=[];const P=(p,c,k)=>s.push(shape(p,c,k)),R=(a,b,c,d,col,k)=>s.push(box(a,b,c,d,col,k)),L=(a,b,c,d,r,col,k)=>s.push(line(a,b,c,d,r,col,k));
 const tool=id.match(/^(dark_crystal|crystal)_(sword|pickaxe|axe|shovel|hoe)$/);
 if(tool){const col=tool[1]==="crystal"?teal:violet,t=tool[2];L(4,28,21,11,2.1,dark);L(4,28,18,14,1.35,rubber,"grip");L(4,28,6,26,2,steel);L(17,15,21,11,2.2,steel);
 if(t==="sword"){P([[10,19],[15,14],[25,3],[30,2],[29,7],[18,18],[13,22]],col,"crystal");L(9,17,17,25,1.5,steel);L(15,18,26,7,.55,pale);}
 if(t==="pickaxe"){P([[8,6],[12,3],[21,4],[28,10],[30,16],[26,15],[22,10],[14,8]],col,"crystal");L(17,8,22,12,1.3,steel);}
 if(t==="axe"){P([[15,4],[22,2],[29,5],[30,11],[26,16],[21,15],[17,11]],col,"crystal");L(19,7,23,12,1.2,steel);}
 if(t==="shovel"){P([[17,9],[23,3],[28,4],[30,9],[24,16],[19,16],[16,13]],col,"crystal");L(19,13,24,7,.65,pale);}
 if(t==="hoe"){P([[14,6],[19,3],[28,4],[30,8],[27,12],[24,10],[25,7],[18,9]],col,"crystal");L(18,9,22,11,1.3,steel);}
 return s;}
 if(id==="titanium_drill"){L(4,27,19,12,2.2,dark,"grip");P([[7,20],[15,9],[20,8],[25,12],[22,19],[14,26],[8,26]],steel);P([[18,8],[25,2],[30,2],[29,7],[23,13]],pale);L(19,9,25,4,.65,dark);L(21,11,27,6,.65,dark);R(10,18,14,21,amber);return s;}
 if(id==="lunar_crystal"||id==="dark_crystal"){let c=id==="lunar_crystal"?teal:violet;P([[5,12],[15,3],[23,6],[28,17],[24,26],[14,30],[5,23]],c,"crystal");P([[15,4],[19,12],[14,27],[8,21],[8,13]],shade(c,36),"crystal");P([[19,12],[23,7],[27,17],[23,25],[14,28]],shade(c,-25),"crystal");return s;}
 if(id==="raw_titanium"||id==="meteorite_fragment"){let c=id==="raw_titanium"?steel:[114,78,61];P([[4,13],[12,5],[22,7],[28,13],[29,22],[22,28],[11,29],[3,22]],c,"stone");P([[7,13],[14,7],[21,9],[17,16],[10,20]],shade(c,25),"stone");P([[17,16],[27,15],[25,24],[19,27]],shade(c,-30),"stone");return s;}
 if(id==="titanium_ingot"){P([[3,17],[9,9],[24,9],[29,16],[27,24],[7,25]],steel);P([[9,10],[24,10],[28,16],[5,17]],pale);R(7,19,26,23,[104,119,128]);return s;}
 if(id==="lunar_stick"){L(5,28,26,4,2.6,[70,81,91],"stone");L(5,28,26,4,1,[128,155,167],"stone");L(11,21,8,13,1.2,[91,111,125],"stone");return s;}
 if(id==="space_helmet"){P([[5,10],[10,4],[22,4],[28,11],[28,24],[23,29],[8,29],[3,23]],pale,"fabric");P([[6,12],[11,8],[23,8],[26,13],[25,22],[7,22]],dark);P([[8,13],[12,10],[23,10],[24,14],[23,20],[9,20]], [48,80,99],"glass");R(7,24,24,26,steel);R(5,18,6,23,amber);return s;}
 if(id==="space_chestplate"){P([[3,6],[10,4],[13,7],[19,7],[22,4],[29,6],[31,14],[25,17],[23,14],[23,29],[9,29],[9,14],[6,17],[1,14]],pale,"fabric");R(10,9,22,25,[168,177,177],"fabric");R(12,11,20,17,dark);R(14,12,18,14,teal);R(12,22,20,25,steel);R(8,27,24,28,amber);return s;}
 if(id==="space_leggings"){P([[5,4],[27,4],[28,28],[19,28],[18,14],[14,14],[13,28],[4,28]],pale,"fabric");R(6,5,26,8,dark);R(7,17,12,22,steel);R(20,17,25,22,steel);return s;}
 if(id==="space_boots"){for(let x of [3,18]){P([[x+2,4],[x+10,4],[x+10,19],[x+13,23],[x+13,28],[x,28],[x,21],[x+2,18]],pale,"fabric");R(x,25,x+13,28,dark);R(x+2,12,x+10,14,amber);}return s;}
 if(id==="oxygen_backpack"){R(4,5,28,28,dark);for(let x of [6,17]){P([[x,9],[x+2,5],[x+7,5],[x+9,9],[x+9,25],[x+6,28],[x+3,28],[x,25]],pale);R(x,15,x+9,19,teal);}R(14,7,18,25,steel);return s;}
 if(id==="lunar_radar"||id==="mineral_detector"){let radar=id==="lunar_radar";if(radar)L(11,9,11,2,1.1,steel);P([[5,7],[25,7],[29,11],[29,25],[24,29],[7,29],[3,25],[3,11]],steel);R(6,10,26,21,dark);R(8,12,24,19,[36,66,66],"glass");if(radar){L(16,12,16,19,.4,teal);L(9,16,23,16,.4,teal);L(16,16,22,13,.6,[160,188,153]);}else{L(10,17,14,14,.5,teal);L(14,14,17,17,.5,teal);L(17,17,22,13,.5,teal);}R(8,24,12,26,amber);R(17,24,23,26,dark);return s;}
 // Pressure vessels: recognizable different capacities and gases.
 const tank=id==="extreme_oxygen_tank"?3:id==="large_oxygen_tank"?2:1,gas=id==="helium_3"?[111,146,109]:id==="rocket_fuel"?amber:teal;
 if(tank===1){P([[11,4],[21,4],[24,8],[25,25],[21,29],[11,29],[7,25],[7,8]],pale);R(13,2,19,5,dark);R(8,11,24,20,gas);R(8,23,24,25,steel);}
 else{for(let i=0;i<tank;i++){let x=4+i*(24/tank),w=24/tank-1;P([[x,8],[x+2,4],[x+w-2,4],[x+w,8],[x+w,26],[x+w-2,29],[x+2,29],[x,26]],pale);R(x,12,x+w,20,gas);R(x+1,3,x+w-1,5,dark);}R(4,23,27,25,steel);}
 return s;
}
function item(id,x,y){const xx=(x+.5)/2,yy=(y+.5)/2;let out=T;for(const layer of layers(id)){if(!layer.test(xx,yy))continue;let edge=!layer.test(xx-.45,yy)||!layer.test(xx+.45,yy)||!layer.test(xx,yy-.45)||!layer.test(xx,yy+.45);let c=layer.col,v=(fb(x,y,14)-.5)*22+grain(x,y)*9+(16-xx)*.65+(16-yy)*.4;if(layer.kind==="grip")v+=Math.floor(xx+yy)%3===0?-14:5;if(layer.kind==="fabric")v+=((x+y)%3===0?-3:2);if(layer.kind==="glass")v+=xx+yy<25?23:-9;if(layer.kind==="crystal")v+=Math.sin((xx-yy)*.45)*9;if(layer.kind==="stone")v+=(fb(x,y,8)-.5)*34;if(edge)v-=32;out=shade(c,v);}return out;}
function armor(src){const old=decode(src);return png(old.w*2,old.h*2,(x,y)=>{const c=old.pixel(x>>1,y>>1);if(c[3]===0)return T;let lum=(c[0]+c[1]+c[2])/3,sat=Math.max(...c.slice(0,3))-Math.min(...c.slice(0,3));let base=sat<32?mix([42,49,55],[209,210,200],lum/255):mix([lum,lum,lum],c,.64);let v=(fb(x%64,y%64,21)-.5)*18+grain(x,y)*8+((x+y)%4===0?-3:1);return C(base[0]+v,base[1]+v,base[2]+v,c[3]);});}
function earth(x,y){const n=128,xx=(x+.5-n)/122,yy=(y+.5-n)/122,r=xx*xx+yy*yy;if(r>1.015)return T;if(r>1){let a=(1.015-r)/.015;return C(89,153,213,a*90);}const z=Math.sqrt(1-r),lat=Math.asin(-yy),lon=Math.atan2(xx,z)+.35;let u=lon*180/Math.PI,v=lat*180/Math.PI;
 // Stylized orthographic Africa/Eurasia and adjacent islands; coastlines distorted by fine noise.
 const f=(fb(x%64,y%64,31)-.5)*5;u+=f;v+=f*.7;
 const africa=[[-18,34],[1,37],[17,31],[32,31],[35,15],[50,10],[42,-12],[31,-34],[18,-35],[10,-20],[0,-5],[-13,4],[-18,15]];
 const eurasia=[[-11,36],[-9,48],[9,56],[24,64],[48,68],[80,72],[120,66],[118,42],[99,20],[80,9],[65,25],[48,30],[38,41],[27,41],[18,35],[5,43]];
 const land=poly(u,v,africa)||poly(u,v,eurasia)||poly(u,v,[[45,-13],[50,-16],[49,-25],[43,-24]]);
 let c=land?mix([77,103,71],[170,145,99],Math.max(0,Math.min(1,.6+Math.sin(v*.12)*.28+(fb(x%64,y%64,9)-.5)*.8))):mix([12,38,69],[44,87,123],fb(x%64,y%64,15));
 if(Math.abs(v)>71)c=mix(c,[206,217,219],.8);
 let cloud=.53*noise(x%64,y%64,16,33)+.3*noise(x%64,y%64,8,34)+.17*noise(x%64,y%64,4,35)+Math.sin(xx*13+yy*23)*.055;
 let opacity=Math.max(0,Math.min(.85,(cloud-.54)*3.2));c=mix(c,[218,225,222],opacity);
 let light=Math.max(.12,.2+.8*(z*.75-xx*.48-yy*.2));c=c.map((v,i)=>v*light);let rim=Math.pow(1-z,3)*.65;c=mix(c,[75,137,190],rim);return C(c[0],c[1],c[2],255);}
function generate(path,src){let id=path.split("/").at(-1).replace(".png","");if(path.includes("/armor/"))return armor(src);if(id==="earth")return png(256,256,earth);if(id==="icon")return png(64,64,(x,y)=>{let r=Math.hypot(x-32,y-32);if(r>30)return C(14,22,30);if(r>27)return C(117,134,144);if(r>23)return C(25,41,54);let a=Math.hypot(x-27,y-30),b=Math.hypot(x-37,y-24);return a<19&&b>17?rock(x,y,"highland_rock"):y>44&&y<47?C(175,117,67):C(24,38,51);});if(path.includes("/item/"))return png(64,64,(x,y)=>item(id,x,y));return png(64,64,(x,y)=>block(id,x,y));}
return {generate,decode,png};
})()
