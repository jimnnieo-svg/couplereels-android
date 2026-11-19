"""Selected PPT images. Publish only processed copies; never copy the ZIP itself."""
from pathlib import Path
from PIL import Image,ImageFilter,ImageDraw
import json
ROOT=Path(__file__).resolve().parents[1];SRC=ROOT.parent/'couplereels-ppt-reference';OUT=ROOT/'public'/'story';OUT.mkdir(exist_ok=True)
manifest=json.loads((SRC/'manifest.json').read_text(encoding='utf8'))
selected=[(22,'case-one',False),(21,'case-temple',False),(20,'case-study',False),(16,'reel-flowers',True),(13,'reel-gift',True),(14,'reel-date',True),(10,'concept-discovery',False),(11,'concept-taste',False)]
used=[]
for index,name,blur in selected:
 item=next(x for x in manifest if x['id']==index);im=Image.open(SRC/item['file']).convert('RGB')
 if blur:
  im=im.filter(ImageFilter.GaussianBlur(radius=32))
  # Strong privacy treatment of all account/UI areas, on the actual file, not a CSS veil.
  d=ImageDraw.Draw(im);d.rectangle((0,int(im.height*.72),im.width,im.height),fill='#e9dce2');d.rectangle((0,0,im.width,int(im.height*.1)),fill='#e9dce2')
 im.thumbnail((1050,1350),Image.Resampling.LANCZOS);im.save(OUT/(name+'.webp'),quality=88,method=6)
 used.append({'source':item['name'],'output':'/story/'+name+'.webp','originalSize':item['size'],'publicSize':im.size,'treatment':'전체 흐림 및 계정 영역 가림' if blur else '원본 비율 유지, 크기 최적화'})
for i,name in [(2,'profiles'),(3,'discovery'),(4,'share'),(5,'library'),(6,'taste')]:
 item=next(x for x in manifest if x['id']==i);used.append({'source':item['name'],'output':'/screens/'+name+'.webp','treatment':'기존 제공본과 동일 화면; 공개용 식별정보/외부 콘텐츠 가림'})
(ROOT/'public'/'image-sources.json').write_text(json.dumps(used,ensure_ascii=False,indent=2),encoding='utf8')
print('8 selected PPT photos prepared; total 13 ZIP images used including five existing app screens')
