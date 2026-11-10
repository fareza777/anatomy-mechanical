import React from 'react';
import {Img,staticFile,useCurrentFrame,useVideoConfig,interpolate} from 'remotion';
import {Base,Copy,clamp} from './design';
export const Hook=()=>{const f=useCurrentFrame();const{fps,width,height}=useVideoConfig();const v=height>width;return <Base><Img src={staticFile('hero.png')} style={{position:'absolute',width:2200,height:v?1100:1080,objectFit:'cover',left:v?-820:0,top:v?740:0,opacity:interpolate(f,[0,.4*fps],[0,.83],clamp),scale:interpolate(f,[0,3*fps],[1.12,1],clamp)}}/><div style={{position:'absolute',inset:0,background:v?'linear-gradient(#071719 30%,transparent 70%)':'linear-gradient(90deg,#071719 5%,#071719ee 26%,transparent 67%)'}}/><Copy tag="LOOK CLOSER." title={['You see','a machine.']} body="What makes it work?"/></Base>}
