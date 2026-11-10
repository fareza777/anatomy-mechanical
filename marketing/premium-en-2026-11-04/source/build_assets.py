from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageOps
from pathlib import Path
import json, shutil, math

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'play-store'; PUB=ROOT/'remotion'/'public'; SRC=Path(r'D:\Anatomy Apps\docs\screenshots')
INK='#071719'; MINT='#8AEED8'; IVORY='#F3F0E7'; TEAL='#116D64'
def font(size, bold=False, mono=False):
    return ImageFont.truetype('C:/Windows/Fonts/'+('consola.ttf' if mono else 'bahnschrift.ttf' if bold else 'segoeui.ttf'),size)
def text(im,xy,s,size,color,bold=False,mono=False):
    ImageDraw.Draw(im).text(xy,s,font=font(size,bold,mono),fill=color,spacing=0)
def grid(im,light=False):
    d=ImageDraw.Draw(im);c='#DFE4DC' if light else '#102C2C'
    for x in range(72,1080,120): d.line((x,420,x,1920),fill=c,width=1)
    for y in range(480,1920,120): d.line((0,y,1080,y),fill=c,width=1)
def rounded_paste(im,layer,xy,r=38):
    mask=Image.new('L',layer.size);ImageDraw.Draw(mask).rounded_rectangle((0,0,layer.width-1,layer.height-1),radius=r,fill=255)
    im.paste(layer,xy,mask)
def phone(im,name,x,y,w):
    raw=Image.open(SRC/(name+'.png')).convert('RGB')
    # Remove only OS status/navigation bars; preserve every app pixel.
    raw=raw.crop((0,round(raw.height*.035),raw.width,round(raw.height*.958)))
    h=round(raw.height*w/raw.width)
    if h>1180:
        w=round(w*1180/h);h=1180;x=(1080-w)//2
    raw=raw.resize((w,h),Image.Resampling.LANCZOS)
    sh=Image.new('RGBA',im.size);ImageDraw.Draw(sh).rounded_rectangle((x-12,y-12,x+w+12,y+h+12),radius=58,fill=(0,0,0,110));sh=sh.filter(ImageFilter.GaussianBlur(23));im.paste(sh,(0,0),sh)
    ImageDraw.Draw(im).rounded_rectangle((x-9,y-9,x+w+9,y+h+9),radius=47,fill='#263B3A',outline='#6A7D76',width=2)
    rounded_paste(im,raw,(x,y),38)
    return h

items=[
 ('01-explore','EXPLORE IN 3D',['See what makes','machines move.'],'Orbit the model. Follow your curiosity.','phone-studio-finalrender',False,'INTERACTIVE 3D ATLAS'),
 ('02-cutaway','LOOK BENEATH THE SURFACE',['The inside story.','Revealed.'],'See through the shell. Explore the structure.','phone-cutaway',True,'CUTAWAY VIEW'),
 ('03-explode','TAKE IT APART',['Every part.','A new perspective.'],'Separate assemblies with a single slider.','phone-explode',False,'EXPLODED VIEW'),
 ('04-simulate','PUT IDEAS IN MOTION',['Static diagrams?','Meet live systems.'],'Run the model. Watch the readouts respond.','phone-run-final',True,'LIVE SIMULATION'),
 ('05-learn','FOLLOW THE EXPLAIN POINTS',['Small details.','Big discoveries.'],'Tap a component to understand its role.','phone-hotspot',False,'GUIDED LEARNING'),
 ('06-robotics','EXPAND YOUR CURIOSITY',['From jet engines','to robotic arms.'],'Explore machines across engineering fields.','phone-robot',True,'MECHANICAL COLLECTION'),
 ('07-export','TAKE THE STUDY WITH YOU',['Your next idea','starts here.'],'Export technical plates as PNG or SVG.','phone-export-done',False,'PNG + SVG EXPORT'),
 ('08-offline','KEEP EXPLORING',['Curiosity travels.','Your atlas too.'],'The 3D collection works offline.','new-home',True,'OFFLINE 3D COLLECTION')
]
for idx,(slug,kicker,head,sub,shot,light,foot) in enumerate(items):
    im=Image.new('RGB',(1080,1920),IVORY if light else INK);grid(im,light)
    fg=INK if light else IVORY;accent=TEAL if light else MINT;muted='#5A6C67' if light else '#A9BDB5'
    d=ImageDraw.Draw(im);d.line((72,70,1008,70),fill=accent,width=2)
    text(im,(72,91),'ANATOMY STUDIO',26,fg,True);text(im,(897,92),f'{idx+1:02d} / 08',20,accent,mono=True)
    text(im,(72,171),kicker,24,accent,mono=True)
    for j,line in enumerate(head):text(im,(66,222+j*99),line,85 if idx!=2 else 80,fg,True)
    text(im,(72,439),sub,31,muted)
    # Different card placement retains a unified editorial rhythm.
    w=626 if idx in [1,4,6] else 652
    h=phone(im,shot,(1080-w)//2,558,w)
    d=ImageDraw.Draw(im);d.line((72,1810,1008,1810),fill=accent,width=1)
    text(im,(72,1835),foot,22,accent,mono=True);text(im,(817,1833),'EXPLORE  /',21,muted,mono=True)
    im.save(OUT/(slug+'.png'),optimize=True)
    shutil.copy2(SRC/(shot+'.png'),PUB/(shot+'.png'))

icon=Image.open(ROOT/'source/icon-master.png').convert('RGB')
icon.resize((512,512),Image.Resampling.LANCZOS).save(OUT/'app-icon-512.png',optimize=True)
icon.resize((1024,1024),Image.Resampling.LANCZOS).save(ROOT/'app-icon-master-1024.png',optimize=True)
shutil.copy2(ROOT/'source/icon-master.png',PUB/'icon.png')
art=Image.open(ROOT/'source/engine-art-master.png').convert('RGB')
banner=ImageOps.fit(art,(2048,1000),method=Image.Resampling.LANCZOS)
text(banner,(105,132),'ANATOMY',100,IVORY,True);text(banner,(105,238),'STUDIO',100,IVORY,True)
text(banner,(109,400),'Go beneath',54,MINT,True);text(banner,(109,465),'the surface.',54,MINT,True)
text(banner,(111,600),'Explore. Take apart.',31,'#C7D6CE');text(banner,(111,647),'Understand.',31,'#C7D6CE')
ImageDraw.Draw(banner).line((110,773,420,773),fill=MINT,width=2)
text(banner,(110,800),'INTERACTIVE 3D ATLAS',23,MINT,mono=True)
banner.save(ROOT/'banner-master-2048x1000.png',optimize=True)
banner.resize((1024,500),Image.Resampling.LANCZOS).save(OUT/'feature-graphic-1024x500.png',optimize=True)
shutil.copy2(ROOT/'source/engine-art-master.png',PUB/'hero.png')
for n in ['new-tf9-compare','new-gearbox','new-delta2','phone-learn','robot-cutaway']:
    shutil.copy2(SRC/(n+'.png'),PUB/(n+'.png'))
board=Image.new('RGB',(1920,1230),INK)
for i,it in enumerate(items):
    im=Image.open(OUT/(it[0]+'.png')).resize((270,480),Image.Resampling.LANCZOS)
    board.paste(im,(65+(i%4)*475,150+(i//4)*535))
text(board,(65,40),'ANATOMY STUDIO  /  PLAY STORE COLLECTION',38,IVORY,True)
board.save(ROOT/'screenshot-collection-preview.jpg',quality=94)
manifest=[]
for p in OUT.glob('*.png'):
    im=Image.open(p);manifest.append({'file':p.name,'width':im.width,'height':im.height,'mode':im.mode,'bytes':p.stat().st_size})
(ROOT/'source/asset-manifest.json').write_text(json.dumps(manifest,indent=2))
print(json.dumps(manifest,indent=2))
