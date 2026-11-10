import React from 'react';
import {registerRoot,Composition,Sequence,AbsoluteFill,useVideoConfig,staticFile,useCurrentFrame,interpolate} from 'remotion';
import {Audio} from '@remotion/media';
import {Hook} from './Hook';import {Reveal} from './Reveal';import {Explore} from './Explore';import {Dissect} from './Dissect';import {Motion} from './Motion';import {Learn} from './Learn';import {Export} from './Export';import {End} from './End';
const Launch=()=>{const{fps}=useVideoConfig();const f=useCurrentFrame();return <AbsoluteFill>
<Sequence name="Hook / The question" from={0} durationInFrames={3*fps} premountFor={fps}><Hook/></Sequence>
<Sequence name="The reveal" from={3*fps} durationInFrames={3*fps} premountFor={fps}><Reveal/></Sequence>
<Sequence name="Explore in 3D" from={6*fps} durationInFrames={5*fps} premountFor={fps}><Explore/></Sequence>
<Sequence name="Cutaway and explode" from={11*fps} durationInFrames={5*fps} premountFor={fps}><Dissect/></Sequence>
<Sequence name="Simulate" from={16*fps} durationInFrames={5*fps} premountFor={fps}><Motion/></Sequence>
<Sequence name="Learn" from={21*fps} durationInFrames={5*fps} premountFor={fps}><Learn/></Sequence>
<Sequence name="Export and offline" from={26*fps} durationInFrames={5*fps} premountFor={fps}><Export/></Sequence>
<Sequence name="Brand and CTA" from={31*fps} durationInFrames={5*fps} premountFor={fps}><End/></Sequence>
<Audio src={staticFile('score-master.wav')} premountFor={fps}/><div style={{position:'absolute',left:0,bottom:0,height:5,width:`${interpolate(f,[0,36*fps],[0,100])}%`,background:'#8AEED8'}}/></AbsoluteFill>}
registerRoot(()=> <><Composition id="AnatomyLaunch" component={Launch} width={1920} height={1080} fps={30} durationInFrames={1080}/><Composition id="AnatomyVertical" component={Launch} width={1080} height={1920} fps={30} durationInFrames={1080}/><Composition id="Hook" component={Hook} width={1920} height={1080} fps={30} durationInFrames={90}/><Composition id="Explore" component={Explore} width={1920} height={1080} fps={30} durationInFrames={150}/><Composition id="Dissect" component={Dissect} width={1920} height={1080} fps={30} durationInFrames={150}/><Composition id="Motion" component={Motion} width={1920} height={1080} fps={30} durationInFrames={150}/><Composition id="Learn" component={Learn} width={1920} height={1080} fps={30} durationInFrames={150}/><Composition id="Export" component={Export} width={1920} height={1080} fps={30} durationInFrames={150}/><Composition id="End" component={End} width={1920} height={1080} fps={30} durationInFrames={150}/></>);
