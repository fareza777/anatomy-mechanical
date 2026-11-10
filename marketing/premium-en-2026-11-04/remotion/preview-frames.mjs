import {bundle} from '@remotion/bundler';
import {selectComposition,renderStill,openBrowser} from '@remotion/renderer';
import {mkdir} from 'node:fs/promises';
const serveUrl=await bundle({entryPoint:'src/index.tsx'});
const browser=await openBrowser('chrome');
await mkdir('../source/verification-frames',{recursive:true});
try{
 for(const id of ['AnatomyLaunch','AnatomyVertical']){
  const composition=await selectComposition({serveUrl,id,puppeteerInstance:browser});
  for(const frame of [60,135,240,420,570,720,870,1020]){
   await renderStill({serveUrl,composition,frame,output:`../source/verification-frames/${id}-${frame}.jpg`,imageFormat:'jpeg',jpegQuality:85,scale:.5,puppeteerInstance:browser});
   console.log(`${id}: ${frame}`);
  }
 }
}finally{await browser.close({silent:true});}
