import React from 'react';
import {AbsoluteFill,Img,staticFile,useCurrentFrame,useVideoConfig,interpolate,Easing} from 'remotion';
export const ink='#071719',paper='#F3F0E7',mint='#8AEED8';
export const ease=Easing.bezier(.16,1,.3,1);
export const clamp={extrapolateLeft:'clamp',extrapolateRight:'clamp'} as const;
export const Fonts=()=> <style>{`@font-face{font-family:Space;src:url('${staticFile('space-grotesk.ttf')}')}@font-face{font-family:Manrope;src:url('${staticFile('manrope.ttf')}')}@font-face{font-family:Jet;src:url('${staticFile('jetbrains-mono.ttf')}')}*{box-sizing:border-box}`}</style>;
export const Base:React.FC<{children?:React.ReactNode,light?:boolean}>=({children,light=false})=>{
 const f=useCurrentFrame(); const {width,height}=useVideoConfig();
 return <AbsoluteFill style={{background:light?paper:ink,color:light?ink:paper,fontFamily:'Space',overflow:'hidden'}}><Fonts/><AbsoluteFill style={{opacity:.4,backgroundImage:`linear-gradient(${light?'#ccd5cd':'#163C3A'} 1px,transparent 1px),linear-gradient(90deg,${light?'#ccd5cd':'#163C3A'} 1px,transparent 1px)`,backgroundSize:'120px 120px',translate:`${-f*.15}px ${f*.08}px`}}/><div style={{position:'absolute',left:90,right:90,top:60,display:'flex',justifyContent:'space-between',fontFamily:'Jet',fontSize:18,letterSpacing:2,color:light?'#116D64':mint}}><span>ANATOMY STUDIO</span><span>{width<height?'3D ATLAS':'A CLOSER LOOK AT HOW THINGS WORK'}</span></div>{children}<div style={{position:'absolute',left:90,right:90,bottom:52,height:1,background:light?'#B7C9BE':'#32524A'}}/></AbsoluteFill>
}
export const Phone:React.FC<{src:string,x:number,y:number,w:number,rotate?:number}>=({src,x,y,w,rotate=0})=>{
 const f=useCurrentFrame();const {fps}=useVideoConfig();
 return <div style={{position:'absolute',left:x,top:y,width:w,border:'3px solid #5D756D',borderRadius:38,overflow:'hidden',background:paper,boxShadow:'0 30px 70px #0006',rotate:`${rotate}deg`,translate:`0 ${interpolate(f,[0,.8*fps],[100,0],{...clamp,easing:ease})+Math.sin(f/38)*5}px`,opacity:interpolate(f,[0,.5*fps],[0,1],clamp)}}><Img src={staticFile(src+'.png')} style={{display:'block',width:'100%',marginTop:'-3.2%',marginBottom:'-3.2%'}}/></div>
}
export const Copy:React.FC<{tag:string,title:string[],body:string,light?:boolean}>=({tag,title,body,light=false})=>{
 const f=useCurrentFrame();const {fps,width,height}=useVideoConfig();const v=height>width;
 return <div style={{position:'absolute',left:90,top:v?180:260,width:v?900:980,opacity:interpolate(f,[0,.6*fps],[0,1],clamp),translate:`0 ${interpolate(f,[0,.8*fps],[45,0],{...clamp,easing:ease})}px`}}><div style={{fontFamily:'Jet',fontSize:v?26:23,letterSpacing:4,color:light?'#116D64':mint,marginBottom:26}}>{tag}</div><div style={{fontSize:v?88:100,lineHeight:1.04,fontWeight:600,letterSpacing:-4}}>{title.map((s,i)=><div key={s} style={{overflow:'hidden'}}><div style={{translate:`0 ${interpolate(f,[i*.12*fps,(.65+i*.12)*fps],[110,0],{...clamp,easing:ease})}%`}}>{s}</div></div>)}</div><div style={{fontFamily:'Manrope',fontSize:v?37:36,lineHeight:1.5,color:light?'#536961':'#AAC4BA',marginTop:30,maxWidth:v?850:840}}>{body}</div></div>
}
