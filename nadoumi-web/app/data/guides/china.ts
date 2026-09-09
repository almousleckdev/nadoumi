/**
 * A concise, professional guide to every province-level division of China for
 * prospective international students: a short history/geography note, a culture /
 * language / festivals note, and two cities each with their leading universities.
 *
 * Grouped by the standard geographic regions, plus the special administrative
 * regions and Taiwan. Content is editorial and kept deliberately brief.
 */

export interface GuideUniversity {
  name: string
  cn?: string
}

export interface GuideCity {
  name: string
  cn: string
  note: string
  universities: GuideUniversity[]
}

export interface Division {
  name: string
  cn: string
  kind: 'Province' | 'Autonomous region' | 'Municipality'
  overview: string
  culture: string
  cities: GuideCity[]
  /**
   * Optional Unsplash path (served via the `unsplash` provider, see
   * `nuxt.config.ts`). Present only for the provinces we have a verified,
   * on-topic photograph for; the rest use a designed graphic hero.
   */
  hero?: string
}

export interface Region {
  id: string
  name: string
  blurb: string
  divisions: Division[]
}

export const CHINA_REGIONS: Region[] = [
  {
    id: 'north',
    name: 'North China',
    blurb: 'The historic political heartland around the capital, on the dry North China Plain.',
    divisions: [
      {
        name: 'Beijing', cn: '北京', kind: 'Municipality',
        hero: '/photo-1547981609-4b6bfe67ca0b',
        overview: 'China\'s capital for most of the last eight centuries, seat of the Yuan, Ming, Qing and the People\'s Republic. Home of the Forbidden City, the Temple of Heaven and the closest stretches of the Great Wall.',
        culture: 'Standard Mandarin (Putonghua) is based on the Beijing dialect. The city keeps Peking opera, hutong courtyard life and Spring Festival temple fairs alongside a fast modern rhythm; Beijing roast duck is its signature dish.',
        cities: [
          { name: 'Haidian District', cn: '海淀区', note: 'The university and technology quarter in the city\'s north-west.', universities: [ { name: 'Peking University', cn: '北京大学' }, { name: 'Tsinghua University', cn: '清华大学' }, { name: 'Renmin University of China', cn: '中国人民大学' } ] },
          { name: 'Chaoyang District', cn: '朝阳区', note: 'The central business and embassy district east of the old city.', universities: [ { name: 'University of International Business and Economics', cn: '对外经济贸易大学' }, { name: 'Communication University of China', cn: '中国传媒大学' } ] },
        ],
      },
      {
        name: 'Tianjin', cn: '天津', kind: 'Municipality',
        overview: 'A port on the Bohai Gulf and Beijing\'s historic gateway to the sea. Nineteenth-century foreign concessions left European avenues along the Haihe river.',
        culture: 'The Tianjin dialect is known for quick wit; the city is the home of xiangsheng (crosstalk comedy). Goubuli steamed buns, clay figurine folk art and New Year woodblock prints are local traditions.',
        cities: [
          { name: 'Nankai District', cn: '南开区', note: 'The academic core beside Nankai Park.', universities: [ { name: 'Nankai University', cn: '南开大学' }, { name: 'Tianjin University', cn: '天津大学' } ] },
          { name: 'Jinnan District', cn: '津南区', note: 'A newer campus and enterprise zone in the south.', universities: [ { name: 'Tiangong University', cn: '天津工业大学' } ] },
        ],
      },
      {
        name: 'Hebei', cn: '河北', kind: 'Province',
        hero: '/photo-1508804185872-d7badad00f7d',
        overview: 'The province that wraps around Beijing and Tianjin. It holds the Chengde imperial summer resort, the Shanhaiguan pass where the Great Wall meets the sea, and much of China\'s steel industry.',
        culture: 'Hebei bangzi is a loud, percussive regional opera; the province has deep martial-arts lineages and elaborate Lantern Festival lantern fairs and ice sculptures.',
        cities: [
          { name: 'Shijiazhuang', cn: '石家庄', note: 'The provincial capital, a railway city on the plain.', universities: [ { name: 'Hebei Normal University', cn: '河北师范大学' }, { name: 'Shijiazhuang Tiedao University', cn: '石家庄铁道大学' } ] },
          { name: 'Baoding', cn: '保定', note: 'A former Qing provincial seat south-west of Beijing.', universities: [ { name: 'Hebei University', cn: '河北大学' }, { name: 'North China Electric Power University', cn: '华北电力大学' } ] },
        ],
      },
      {
        name: 'Shanxi', cn: '山西', kind: 'Province',
        overview: 'A loess plateau east of the Yellow River and one of the oldest centres of Chinese civilisation. It keeps the walled Ming city of Pingyao, the Yungang Buddhist grottoes, and the legacy of the Jin merchants who ran China\'s first banks.',
        culture: 'The Jin dialect group is distinct within Mandarin. Shanxi is famous for hand-sliced noodles and aged vinegar, for shadow-puppet theatre and for spectacular Lantern Festival displays.',
        cities: [
          { name: 'Taiyuan', cn: '太原', note: 'The industrial provincial capital in the Fen river valley.', universities: [ { name: 'Taiyuan University of Technology', cn: '太原理工大学' }, { name: 'Shanxi University', cn: '山西大学' } ] },
          { name: 'Datong', cn: '大同', note: 'A former northern frontier capital, gateway to the Yungang Grottoes.', universities: [ { name: 'Shanxi Datong University', cn: '山西大同大学' } ] },
        ],
      },
      {
        name: 'Inner Mongolia', cn: '内蒙古', kind: 'Autonomous region',
        overview: 'A long band of grassland, desert and forest along China\'s northern border, with major coal and rare-earth reserves. The mausoleum of Genghis Khan stands in its Ordos plateau.',
        culture: 'Mongolian is co-official with Mandarin and written in the vertical Mongol script. The summer Naadam festival features wrestling, horse racing and archery; throat singing, the morin khuur fiddle and milk tea are everyday culture.',
        cities: [
          { name: 'Hohhot', cn: '呼和浩特', note: 'The regional capital, a city of temples on the steppe edge.', universities: [ { name: 'Inner Mongolia University', cn: '内蒙古大学' } ] },
          { name: 'Baotou', cn: '包头', note: 'A steel and rare-earth industrial hub on the Yellow River.', universities: [ { name: 'Inner Mongolia University of Science and Technology', cn: '内蒙古科技大学' } ] },
        ],
      },
    ],
  },
  {
    id: 'northeast',
    name: 'Northeast China',
    blurb: 'The old Manchu homeland and China\'s first heavy-industrial base, cold winters, hearty food, warm humour.',
    divisions: [
      {
        name: 'Liaoning', cn: '辽宁', kind: 'Province',
        overview: 'The southernmost and most industrial of the north-eastern provinces. Shenyang holds the early-Qing Imperial Palace; Dalian is a mild coastal city built by Russia and Japan.',
        culture: 'The broad Northeastern Mandarin accent is instantly recognisable nationwide. Errenzhuan song-and-dance comedy, thick stews, pickled cabbage and winter ice festivals define the region.',
        cities: [
          { name: 'Shenyang', cn: '沈阳', note: 'The provincial capital and largest city of the north-east.', universities: [ { name: 'Northeastern University', cn: '东北大学' }, { name: 'Liaoning University', cn: '辽宁大学' } ] },
          { name: 'Dalian', cn: '大连', note: 'A port and beach city on the Yellow Sea, known for its squares and trams.', universities: [ { name: 'Dalian University of Technology', cn: '大连理工大学' }, { name: 'Dalian Maritime University', cn: '大连海事大学' } ] },
        ],
      },
      {
        name: 'Jilin', cn: '吉林', kind: 'Province',
        overview: 'Centred on the Changbai Mountains and their crater lake on the North Korean border. Changchun grew up around the car and film industries; Yanbian is a Korean-Chinese autonomous prefecture.',
        culture: 'Mandarin is spoken alongside Korean in Yanbian. The winter rime ice on the Songhua river draws photographers; Manchu and Korean dishes, cold noodles, kimchi and barbecue are staples.',
        cities: [
          { name: 'Changchun', cn: '长春', note: 'The provincial capital, a planned industrial and university city.', universities: [ { name: 'Jilin University', cn: '吉林大学' }, { name: 'Northeast Normal University', cn: '东北师范大学' } ] },
          { name: 'Yanji', cn: '延吉', note: 'The seat of the Yanbian Korean Autonomous Prefecture.', universities: [ { name: 'Yanbian University', cn: '延边大学' } ] },
        ],
      },
      {
        name: 'Heilongjiang', cn: '黑龙江', kind: 'Province',
        overview: 'China\'s far north-east, bordering Russia along the Amur river, with the Daqing oilfield and vast black-soil farmland. Harbin was built by the Trans-Manchurian Railway and still shows Russian architecture.',
        culture: 'Harbin\'s International Ice and Snow Sculpture Festival is the world\'s largest. Russian loanwords, sausage and rye bread, winter swimming and hot-pot warm the long winters.',
        cities: [
          { name: 'Harbin', cn: '哈尔滨', note: 'The provincial capital on the Songhua river.', universities: [ { name: 'Harbin Institute of Technology', cn: '哈尔滨工业大学' }, { name: 'Harbin Engineering University', cn: '哈尔滨工程大学' } ] },
          { name: 'Daqing', cn: '大庆', note: 'A city that grew from China\'s most famous oilfield.', universities: [ { name: 'Northeast Petroleum University', cn: '东北石油大学' } ] },
        ],
      },
    ],
  },
  {
    id: 'east',
    name: 'East China',
    blurb: 'The wealthy Yangtze delta and the coast, classical gardens, water towns, and China\'s densest concentration of universities.',
    divisions: [
      {
        name: 'Shanghai', cn: '上海', kind: 'Municipality',
        hero: '/photo-1545893835-abaa50cbe628',
        overview: 'A fishing town that became a treaty port in 1843 and is now China\'s financial capital. The Bund faces the Pudong skyline across the Huangpu river; shikumen lane houses survive between the towers.',
        culture: 'Shanghainese is a Wu dialect. The city\'s haipai ("Shanghai style") culture mixes Chinese and Western influences; xiaolongbao soup dumplings and the Yu Garden Lantern Festival are local touchstones.',
        cities: [
          { name: 'Yangpu District', cn: '杨浦区', note: 'A north-eastern district built around its universities and a redeveloped riverfront.', universities: [ { name: 'Fudan University', cn: '复旦大学' }, { name: 'Tongji University', cn: '同济大学' } ] },
          { name: 'Minhang District', cn: '闵行区', note: 'A large southern district with major campuses and industry.', universities: [ { name: 'Shanghai Jiao Tong University', cn: '上海交通大学' }, { name: 'East China Normal University', cn: '华东师范大学' } ] },
        ],
      },
      {
        name: 'Jiangsu', cn: '江苏', kind: 'Province',
        overview: 'The prosperous province immediately upriver and inland from Shanghai. Nanjing served as a dynastic capital many times; Suzhou\'s classical gardens and canals are a UNESCO site on the Grand Canal.',
        culture: 'Wu and Jianghuai Mandarin are both spoken. Kunqu, the refined opera that shaped all later Chinese theatre, was born here, as was the delicate Huaiyang cuisine.',
        cities: [
          { name: 'Nanjing', cn: '南京', note: 'The provincial capital, a walled city on the Yangtze with layers of history.', universities: [ { name: 'Nanjing University', cn: '南京大学' }, { name: 'Southeast University', cn: '东南大学' } ] },
          { name: 'Suzhou', cn: '苏州', note: 'A canal city of gardens, silk and, today, high technology.', universities: [ { name: 'Soochow University', cn: '苏州大学' }, { name: 'Xi\'an Jiaotong-Liverpool University', cn: '西交利物浦大学' } ] },
        ],
      },
      {
        name: 'Zhejiang', cn: '浙江', kind: 'Province',
        overview: 'A hilly coastal province of tea, silk and private enterprise. Hangzhou\'s West Lake has inspired poets for a thousand years; Mount Putuo is a major Buddhist pilgrimage island.',
        culture: 'Wu dialects vary sharply from valley to valley. Yue opera, Longjing green tea, and watching the autumn tidal bore roar up the Qiantang river are Zhejiang traditions.',
        cities: [
          { name: 'Hangzhou', cn: '杭州', note: 'The provincial capital and a technology centre beside West Lake.', universities: [ { name: 'Zhejiang University', cn: '浙江大学' } ] },
          { name: 'Ningbo', cn: '宁波', note: 'An ancient trading port and one of the world\'s busiest cargo harbours.', universities: [ { name: 'Ningbo University', cn: '宁波大学' }, { name: 'University of Nottingham Ningbo China', cn: '宁波诺丁汉大学' } ] },
        ],
      },
      {
        name: 'Anhui', cn: '安徽', kind: 'Province',
        overview: 'An inland province split by the Yangtze and Huai rivers. Huangshan (the Yellow Mountain) and the whitewashed Huizhou villages of Xidi and Hongcun are UNESCO sites.',
        culture: 'The Huizhou merchants built China\'s ink-stones, rice paper and brush culture. Anhui opera travelled to Beijing in 1790 and became the ancestor of Peking opera.',
        cities: [
          { name: 'Hefei', cn: '合肥', note: 'The provincial capital and a national science hub.', universities: [ { name: 'University of Science and Technology of China', cn: '中国科学技术大学' }, { name: 'Hefei University of Technology', cn: '合肥工业大学' } ] },
          { name: 'Wuhu', cn: '芜湖', note: 'A Yangtze river port and manufacturing city.', universities: [ { name: 'Anhui Normal University', cn: '安徽师范大学' } ] },
        ],
      },
      {
        name: 'Fujian', cn: '福建', kind: 'Province',
        overview: 'A mountainous coast facing Taiwan, historically a launch point for emigration across South-East Asia. Quanzhou was a great medieval maritime-Silk-Road port; the Hakka tulou earthen roundhouses are a UNESCO site.',
        culture: 'The Min dialects, Hokkien in the south, Fuzhou speech in the north, differ greatly from Mandarin. Oolong tea, glove-puppet theatre and the Mazu sea-goddess festival are central to local life.',
        cities: [
          { name: 'Fuzhou', cn: '福州', note: 'The provincial capital, a city of banyan trees and hot springs.', universities: [ { name: 'Fuzhou University', cn: '福州大学' } ] },
          { name: 'Xiamen', cn: '厦门', note: 'An island city with a colonial quarter on Gulangyu and a mild climate.', universities: [ { name: 'Xiamen University', cn: '厦门大学' } ] },
        ],
      },
      {
        name: 'Jiangxi', cn: '江西', kind: 'Province',
        overview: 'A basin drained by the Gan river into Poyang, China\'s largest freshwater lake. Jingdezhen has produced imperial porcelain for a thousand years; Jinggangshan was the first Communist rural base.',
        culture: 'Gan and Hakka are the main dialects. Nuo masked ritual dance, ancestral-hall clan traditions and Dragon Boat racing are strong here.',
        cities: [
          { name: 'Nanchang', cn: '南昌', note: 'The provincial capital, a river city with a revolutionary history.', universities: [ { name: 'Nanchang University', cn: '南昌大学' } ] },
          { name: 'Jingdezhen', cn: '景德镇', note: 'The "porcelain capital" of China.', universities: [ { name: 'Jingdezhen Ceramic University', cn: '景德镇陶瓷大学' } ] },
        ],
      },
      {
        name: 'Shandong', cn: '山东', kind: 'Province',
        overview: 'A peninsula between the Bohai and Yellow seas, and the birthplace of Confucius and Mencius. Mount Tai is the most revered of China\'s sacred peaks; Qingdao keeps a German quarter and a famous brewery.',
        culture: 'Jilu Mandarin is the local speech. Lu cuisine is one of the four great regional traditions; Weifang holds an international kite festival every spring.',
        cities: [
          { name: 'Jinan', cn: '济南', note: 'The "city of springs" and provincial capital.', universities: [ { name: 'Shandong University', cn: '山东大学' } ] },
          { name: 'Qingdao', cn: '青岛', note: 'A coastal city of red roofs, beaches and beer.', universities: [ { name: 'Ocean University of China', cn: '中国海洋大学' }, { name: 'China University of Petroleum (East China)', cn: '中国石油大学（华东）' } ] },
        ],
      },
    ],
  },
  {
    id: 'central',
    name: 'Central China',
    blurb: 'The middle Yangtze and the Central Plains, the oldest cradle of Chinese civilisation.',
    divisions: [
      {
        name: 'Henan', cn: '河南', kind: 'Province',
        overview: 'The core of the Central Plains where Chinese civilisation formed. It holds three ancient capitals (Luoyang, Kaifeng and Anyang, where the oracle bones were found), the Shaolin Temple and the Longmen Grottoes.',
        culture: 'Central Plains Mandarin is the local speech. Yu opera is the country\'s largest regional opera form; wheat noodles and stuffed flatbreads are the everyday diet.',
        cities: [
          { name: 'Zhengzhou', cn: '郑州', note: 'A major railway junction and the provincial capital.', universities: [ { name: 'Zhengzhou University', cn: '郑州大学' } ] },
          { name: 'Kaifeng', cn: '开封', note: 'A former Song-dynasty capital on the Yellow River.', universities: [ { name: 'Henan University', cn: '河南大学' } ] },
        ],
      },
      {
        name: 'Hubei', cn: '湖北', kind: 'Province',
        overview: 'Where the Han river joins the Yangtze at Wuhan, the transport heart of China. The 1911 revolution began here; upriver stands the Three Gorges Dam.',
        culture: 'South-western Mandarin and the ancient Chu heritage shape local identity. Hot-dry noodles (reganmian) are Wuhan\'s breakfast; Dragon Boat racing is said to have started on these rivers.',
        cities: [
          { name: 'Wuhan', cn: '武汉', note: 'The provincial capital and one of China\'s largest inland cities.', universities: [ { name: 'Wuhan University', cn: '武汉大学' }, { name: 'Huazhong University of Science and Technology', cn: '华中科技大学' } ] },
          { name: 'Yichang', cn: '宜昌', note: 'The city beside the Three Gorges Dam.', universities: [ { name: 'China Three Gorges University', cn: '三峡大学' } ] },
        ],
      },
      {
        name: 'Hunan', cn: '湖南', kind: 'Province',
        overview: 'A province of lakes and mountains south of the Yangtze. Dongting Lake, the quartz pillars of Zhangjiajie and Mao Zedong\'s home village of Shaoshan are all here, as is the thousand-year-old Yuelu Academy.',
        culture: 'The Xiang dialect is spoken across the province. Flower-drum folk opera, fiercely chilli-hot cuisine and the festivals of the Miao and Tujia peoples give Hunan a bold character.',
        cities: [
          { name: 'Changsha', cn: '长沙', note: 'The lively provincial capital on the Xiang river.', universities: [ { name: 'Central South University', cn: '中南大学' }, { name: 'Hunan University', cn: '湖南大学' }, { name: 'National University of Defense Technology', cn: '国防科技大学' } ] },
          { name: 'Xiangtan', cn: '湘潭', note: 'An industrial city near Shaoshan.', universities: [ { name: 'Xiangtan University', cn: '湘潭大学' } ] },
        ],
      },
    ],
  },
  {
    id: 'south',
    name: 'South China',
    blurb: 'The subtropical Pearl River delta and the far south, trade, Cantonese culture and China\'s tropical island.',
    divisions: [
      {
        name: 'Guangdong', cn: '广东', kind: 'Province',
        overview: 'The Pearl River delta, China\'s manufacturing and export engine. Guangzhou (Canton) has been a foreign-trade port for two thousand years; Shenzhen went from village to megacity in forty.',
        culture: 'Cantonese, Teochew and Hakka are all spoken. Cantonese opera, lion dance, spring flower markets and, above all, dim sum "yum cha" tea lunches are the culture of Lingnan.',
        cities: [
          { name: 'Guangzhou', cn: '广州', note: 'The provincial capital and southern China\'s great trading city.', universities: [ { name: 'Sun Yat-sen University', cn: '中山大学' }, { name: 'South China University of Technology', cn: '华南理工大学' } ] },
          { name: 'Shenzhen', cn: '深圳', note: 'A young technology metropolis on the Hong Kong border.', universities: [ { name: 'Shenzhen University', cn: '深圳大学' }, { name: 'Southern University of Science and Technology', cn: '南方科技大学' } ] },
        ],
      },
      {
        name: 'Guangxi', cn: '广西', kind: 'Autonomous region',
        hero: '/photo-1537531383496-f4749b8032cf',
        overview: 'A Zhuang autonomous region of dramatic karst scenery, the peaks of Guilin, the Li river, and the Longsheng rice terraces. The Zhuang are China\'s largest ethnic minority.',
        culture: 'The Zhuang language is co-official; South-western Mandarin and Cantonese are also common. The Zhuang "March Third" song festival, along with Yao and Miao customs, fills the spring calendar.',
        cities: [
          { name: 'Nanning', cn: '南宁', note: 'The green regional capital near the Vietnam border.', universities: [ { name: 'Guangxi University', cn: '广西大学' } ] },
          { name: 'Guilin', cn: '桂林', note: 'The tourism city among the limestone peaks.', universities: [ { name: 'Guangxi Normal University', cn: '广西师范大学' }, { name: 'Guilin University of Technology', cn: '桂林理工大学' } ] },
        ],
      },
      {
        name: 'Hainan', cn: '海南', kind: 'Province',
        overview: 'China\'s tropical island province and a new free-trade port. Sanya on the southern tip is a beach-resort city; the interior is home to the Li and Miao peoples.',
        culture: 'Hainanese (a Min dialect) is spoken alongside Mandarin. Li brocade weaving, coconut-shell carving and open-air "Junpo" folk-song gatherings are island traditions.',
        cities: [
          { name: 'Haikou', cn: '海口', note: 'The provincial capital on the north coast.', universities: [ { name: 'Hainan University', cn: '海南大学' } ] },
          { name: 'Sanya', cn: '三亚', note: 'The tropical resort city of the far south.', universities: [ { name: 'Hainan Tropical Ocean University', cn: '海南热带海洋学院' } ] },
        ],
      },
    ],
  },
  {
    id: 'southwest',
    name: 'Southwest China',
    blurb: 'Mountains, plateaus and the greatest ethnic diversity in the country, from Sichuan\'s plains to the Tibetan highlands.',
    divisions: [
      {
        name: 'Chongqing', cn: '重庆', kind: 'Municipality',
        overview: 'A mountain city where the Jialing meets the Yangtze, and China\'s wartime capital from 1937 to 1945. Roads, railways and buildings stack up the cliffs on many levels.',
        culture: 'The Chongqing dialect of South-western Mandarin is fast and direct. The city is the home of numbing-spicy hotpot; Ciqikou old town and Spring Festival keep older customs alive.',
        cities: [
          { name: 'Shapingba District', cn: '沙坪坝区', note: 'The main university district in the city\'s west.', universities: [ { name: 'Chongqing University', cn: '重庆大学' }, { name: 'Southwest University', cn: '西南大学' } ] },
          { name: 'Yuzhong District', cn: '渝中区', note: 'The peninsula that forms the historic city centre.', universities: [ { name: 'Chongqing Medical University', cn: '重庆医科大学' } ] },
        ],
      },
      {
        name: 'Sichuan', cn: '四川', kind: 'Province',
        overview: 'The "land of abundance", the Chengdu plain has been irrigated by the Dujiangyan works for over two thousand years. It holds the giant pandas, Jiuzhaigou\'s lakes, and the Leshan Giant Buddha.',
        culture: 'Sichuanese, a South-western Mandarin dialect, is warm and playful. Sichuan opera with its lightning "face-changing", unhurried teahouse life and mouth-numbing peppercorn cuisine are famous nationwide.',
        cities: [
          { name: 'Chengdu', cn: '成都', note: 'The relaxed provincial capital and a technology centre.', universities: [ { name: 'Sichuan University', cn: '四川大学' }, { name: 'University of Electronic Science and Technology of China', cn: '电子科技大学' }, { name: 'Southwest Jiaotong University', cn: '西南交通大学' } ] },
          { name: 'Mianyang', cn: '绵阳', note: 'China\'s designated "science city", north-east of Chengdu, and Nadoumi\'s home.', universities: [ { name: 'Southwest University of Science and Technology', cn: '西南科技大学' } ] },
        ],
      },
      {
        name: 'Guizhou', cn: '贵州', kind: 'Province',
        overview: 'A rugged, cave-riddled plateau and one of China\'s most ethnically varied provinces. Huangguoshu is the country\'s largest waterfall; Moutai, the national spirit, is distilled here.',
        culture: 'Mandarin shares the province with Miao, Dong and Buyi languages. Dong "grand choirs" are a UNESCO art; the Lusheng reed-pipe festivals and the Miao Sisters\' Meal festival draw whole valleys together.',
        cities: [
          { name: 'Guiyang', cn: '贵阳', note: 'The cool, forested provincial capital and a big-data centre.', universities: [ { name: 'Guizhou University', cn: '贵州大学' } ] },
          { name: 'Zunyi', cn: '遵义', note: 'A historic city where the Communist Party held a pivotal 1935 meeting.', universities: [ { name: 'Zunyi Medical University', cn: '遵义医科大学' } ] },
        ],
      },
      {
        name: 'Yunnan', cn: '云南', kind: 'Province',
        overview: 'China\'s most biodiverse province, on the borders of Myanmar, Laos and Vietnam. It keeps the old towns of Lijiang and Dali, the Shangri-La highlands and the Xishuangbanna rainforest, and it is the source of Pu\'er tea.',
        culture: 'Twenty-five ethnic minorities live here; Bai, Naxi (with its Dongba pictographs), Dai and Yi are among their languages. The Dai Water-Splashing Festival, the Yi Torch Festival and the Dali Third Month Fair are the year\'s highlights.',
        cities: [
          { name: 'Kunming', cn: '昆明', note: 'The mild "spring city" and provincial capital.', universities: [ { name: 'Yunnan University', cn: '云南大学' }, { name: 'Kunming University of Science and Technology', cn: '昆明理工大学' } ] },
          { name: 'Dali', cn: '大理', note: 'A lakeside old town and centre of Bai culture.', universities: [ { name: 'Dali University', cn: '大理大学' } ] },
        ],
      },
      {
        name: 'Tibet', cn: '西藏', kind: 'Autonomous region',
        overview: 'The high plateau, averaging over four thousand metres. Lhasa holds the Potala Palace and the Jokhang Temple; the north face of Mount Everest rises on its southern border.',
        culture: 'Tibetan is co-official and written in its own script. Tibetan Buddhism shapes daily life; Losar (New Year), the Shoton "yoghurt festival" with its giant unveiled thangka, butter tea and tsampa are central traditions.',
        cities: [
          { name: 'Lhasa', cn: '拉萨', note: 'The regional capital and spiritual centre of Tibet.', universities: [ { name: 'Tibet University', cn: '西藏大学' } ] },
          { name: 'Nyingchi', cn: '林芝', note: 'A lower, greener valley region in the south-east.', universities: [ { name: 'Tibet Agricultural and Animal Husbandry University', cn: '西藏农牧学院' } ] },
        ],
      },
    ],
  },
  {
    id: 'northwest',
    name: 'Northwest China',
    blurb: 'The Silk Road corridor, desert oases, grassland, Islam and Buddhism, and the ancient capital of Xi\'an.',
    divisions: [
      {
        name: 'Shaanxi', cn: '陕西', kind: 'Province',
        overview: 'The cradle of imperial China. Xi\'an, as ancient Chang\'an, was the capital of the Zhou, Qin, Han and Tang and the eastern end of the Silk Road; the Terracotta Army, the intact city wall and Mount Hua are all here.',
        culture: 'The Guanzhong dialect of Central Plains Mandarin is spoken around Xi\'an. Qinqiang is one of China\'s oldest operas; paper-cutting, the Lantern Festival, and wide "biangbiang" noodles are Shaanxi hallmarks.',
        cities: [
          { name: 'Xi\'an', cn: '西安', note: 'The provincial capital and one of the great historic cities of the world.', universities: [ { name: 'Xi\'an Jiaotong University', cn: '西安交通大学' }, { name: 'Northwestern Polytechnical University', cn: '西北工业大学' }, { name: 'Xidian University', cn: '西安电子科技大学' } ] },
          { name: 'Yangling', cn: '杨凌', note: 'A national agriculture-technology zone west of Xi\'an.', universities: [ { name: 'Northwest A&F University', cn: '西北农林科技大学' } ] },
        ],
      },
      {
        name: 'Gansu', cn: '甘肃', kind: 'Province',
        overview: 'A long province along the Hexi Corridor, the narrow Silk Road passage between desert and mountains. Dunhuang\'s Mogao caves hold a thousand years of Buddhist murals; Jiayuguan fort marks the western end of the Ming Great Wall.',
        culture: 'Central Plains and Lanyin Mandarin meet Tibetan and Dongxiang here. Hua\'er mountain songs, Dunhuang-inspired dance, and Lanzhou\'s hand-pulled beef noodle soup are the local culture.',
        cities: [
          { name: 'Lanzhou', cn: '兰州', note: 'The provincial capital, strung along the Yellow River.', universities: [ { name: 'Lanzhou University', cn: '兰州大学' } ] },
          { name: 'Tianshui', cn: '天水', note: 'An ancient city near the Maijishan Grottoes.', universities: [ { name: 'Tianshui Normal University', cn: '天水师范学院' } ] },
        ],
      },
      {
        name: 'Qinghai', cn: '青海', kind: 'Province',
        overview: 'A high, thinly populated province that gives rise to the Yangtze, Yellow and Mekong rivers. Qinghai Lake is China\'s largest; Kumbum is a major Tibetan-Buddhist monastery.',
        culture: 'Mandarin, Tibetan and Mongolian are all spoken. Pilgrims circle Qinghai Lake on foot; Tibetan opera, and the international cycling race around the lake each summer, mark the calendar.',
        cities: [
          { name: 'Xining', cn: '西宁', note: 'The provincial capital and the plateau\'s largest city.', universities: [ { name: 'Qinghai University', cn: '青海大学' } ] },
          { name: 'Golmud', cn: '格尔木', note: 'A desert railway town on the line to Tibet.', universities: [ { name: 'Qinghai University (Golmud campus)', cn: '青海大学格尔木校区' } ] },
        ],
      },
      {
        name: 'Ningxia', cn: '宁夏', kind: 'Autonomous region',
        overview: 'A small Hui autonomous region where the Yellow River creates a ribbon of farmland through dry country. The Western Xia royal tombs, Helan Mountain rock art and a fast-growing wine industry are its landmarks.',
        culture: 'Zhongyuan Mandarin is spoken, with a strong Hui Muslim heritage, mosques, halal cuisine, and the widely observed Eid al-Fitr and Eid al-Adha.',
        cities: [
          { name: 'Yinchuan', cn: '银川', note: 'The regional capital between the river and the mountains.', universities: [ { name: 'Ningxia University', cn: '宁夏大学' } ] },
          { name: 'Zhongwei', cn: '中卫', note: 'A town where the desert meets the Yellow River at Shapotou.', universities: [ { name: 'Ningxia University (Zhongwei campus)', cn: '宁夏大学中卫校区' } ] },
        ],
      },
      {
        name: 'Xinjiang', cn: '新疆', kind: 'Autonomous region',
        overview: 'China\'s largest region, a Silk Road crossroads of the Tianshan mountains and the Taklamakan desert. Turpan is a below-sea-level oasis; Kashgar\'s old bazaar has traded for two millennia.',
        culture: 'Uyghur, a Turkic language, is co-official with Mandarin; Kazakh and Kyrgyz are also spoken. The Twelve Muqam song suites are a UNESCO art; laghman noodles, pilaf and naan, and the festivals of Corban (Eid) and Nowruz define the year.',
        cities: [
          { name: 'Ürümqi', cn: '乌鲁木齐', note: 'The regional capital, said to be the city furthest from any ocean.', universities: [ { name: 'Xinjiang University', cn: '新疆大学' } ] },
          { name: 'Kashgar', cn: '喀什', note: 'The historic oasis city of the far west.', universities: [ { name: 'Kashgar University', cn: '喀什大学' } ] },
        ],
      },
    ],
  },
]

export const CHINA_DIVISION_COUNT = CHINA_REGIONS.reduce((n, r) => n + r.divisions.length, 0)
