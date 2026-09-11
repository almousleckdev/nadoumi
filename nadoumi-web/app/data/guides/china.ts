/**
 * Structural data for every province-level division of China, for the City
 * Guides page: grouped by region, each division with two cities and their
 * leading universities.
 *
 * Place names and university names are kept as their standard English/Chinese
 * forms across every locale (this is how they appear in the universities'
 * own English-taught programs and in international usage). The descriptive
 * prose (division overview/culture, city notes, "kind" label) is translated
 * and lives in `i18n/locales/*.json` under `guides.cityGuides`, keyed by
 * `slug` (division) and `id` (city) below.
 */

export type DivisionKind = 'province' | 'autonomousRegion' | 'municipality'

export interface GuideUniversity {
  name: string
  cn?: string
}

export interface GuideCity {
  id: string
  name: string
  cn: string
  universities: GuideUniversity[]
}

export interface Division {
  slug: string
  name: string
  cn: string
  kind: DivisionKind
  cities: GuideCity[]
}

export interface Region {
  id: string
  name: string
  divisions: Division[]
}

export const CHINA_REGIONS: Region[] = [
  {
    id: 'north',
    name: 'North China',
    divisions: [
      {
        slug: 'beijing', name: 'Beijing', cn: '北京', kind: 'municipality',
        cities: [
          { id: 'haidian-district', name: 'Haidian District', cn: '海淀区', universities: [ { name: 'Peking University', cn: '北京大学' }, { name: 'Tsinghua University', cn: '清华大学' }, { name: 'Renmin University of China', cn: '中国人民大学' } ] },
          { id: 'chaoyang-district', name: 'Chaoyang District', cn: '朝阳区', universities: [ { name: 'University of International Business and Economics', cn: '对外经济贸易大学' }, { name: 'Communication University of China', cn: '中国传媒大学' } ] },
        ],
      },
      {
        slug: 'tianjin', name: 'Tianjin', cn: '天津', kind: 'municipality',
        cities: [
          { id: 'nankai-district', name: 'Nankai District', cn: '南开区', universities: [ { name: 'Nankai University', cn: '南开大学' }, { name: 'Tianjin University', cn: '天津大学' } ] },
          { id: 'jinnan-district', name: 'Jinnan District', cn: '津南区', universities: [ { name: 'Tiangong University', cn: '天津工业大学' } ] },
        ],
      },
      {
        slug: 'hebei', name: 'Hebei', cn: '河北', kind: 'province',
        cities: [
          { id: 'shijiazhuang', name: 'Shijiazhuang', cn: '石家庄', universities: [ { name: 'Hebei Normal University', cn: '河北师范大学' }, { name: 'Shijiazhuang Tiedao University', cn: '石家庄铁道大学' } ] },
          { id: 'baoding', name: 'Baoding', cn: '保定', universities: [ { name: 'Hebei University', cn: '河北大学' }, { name: 'North China Electric Power University', cn: '华北电力大学' } ] },
        ],
      },
      {
        slug: 'shanxi', name: 'Shanxi', cn: '山西', kind: 'province',
        cities: [
          { id: 'taiyuan', name: 'Taiyuan', cn: '太原', universities: [ { name: 'Taiyuan University of Technology', cn: '太原理工大学' }, { name: 'Shanxi University', cn: '山西大学' } ] },
          { id: 'datong', name: 'Datong', cn: '大同', universities: [ { name: 'Shanxi Datong University', cn: '山西大同大学' } ] },
        ],
      },
      {
        slug: 'inner-mongolia', name: 'Inner Mongolia', cn: '内蒙古', kind: 'autonomousRegion',
        cities: [
          { id: 'hohhot', name: 'Hohhot', cn: '呼和浩特', universities: [ { name: 'Inner Mongolia University', cn: '内蒙古大学' } ] },
          { id: 'baotou', name: 'Baotou', cn: '包头', universities: [ { name: 'Inner Mongolia University of Science and Technology', cn: '内蒙古科技大学' } ] },
        ],
      },
    ],
  },
  {
    id: 'northeast',
    name: 'Northeast China',
    divisions: [
      {
        slug: 'liaoning', name: 'Liaoning', cn: '辽宁', kind: 'province',
        cities: [
          { id: 'shenyang', name: 'Shenyang', cn: '沈阳', universities: [ { name: 'Northeastern University', cn: '东北大学' }, { name: 'Liaoning University', cn: '辽宁大学' } ] },
          { id: 'dalian', name: 'Dalian', cn: '大连', universities: [ { name: 'Dalian University of Technology', cn: '大连理工大学' }, { name: 'Dalian Maritime University', cn: '大连海事大学' } ] },
        ],
      },
      {
        slug: 'jilin', name: 'Jilin', cn: '吉林', kind: 'province',
        cities: [
          { id: 'changchun', name: 'Changchun', cn: '长春', universities: [ { name: 'Jilin University', cn: '吉林大学' }, { name: 'Northeast Normal University', cn: '东北师范大学' } ] },
          { id: 'yanji', name: 'Yanji', cn: '延吉', universities: [ { name: 'Yanbian University', cn: '延边大学' } ] },
        ],
      },
      {
        slug: 'heilongjiang', name: 'Heilongjiang', cn: '黑龙江', kind: 'province',
        cities: [
          { id: 'harbin', name: 'Harbin', cn: '哈尔滨', universities: [ { name: 'Harbin Institute of Technology', cn: '哈尔滨工业大学' }, { name: 'Harbin Engineering University', cn: '哈尔滨工程大学' } ] },
          { id: 'daqing', name: 'Daqing', cn: '大庆', universities: [ { name: 'Northeast Petroleum University', cn: '东北石油大学' } ] },
        ],
      },
    ],
  },
  {
    id: 'east',
    name: 'East China',
    divisions: [
      {
        slug: 'shanghai', name: 'Shanghai', cn: '上海', kind: 'municipality',
        cities: [
          { id: 'yangpu-district', name: 'Yangpu District', cn: '杨浦区', universities: [ { name: 'Fudan University', cn: '复旦大学' }, { name: 'Tongji University', cn: '同济大学' } ] },
          { id: 'minhang-district', name: 'Minhang District', cn: '闵行区', universities: [ { name: 'Shanghai Jiao Tong University', cn: '上海交通大学' }, { name: 'East China Normal University', cn: '华东师范大学' } ] },
        ],
      },
      {
        slug: 'jiangsu', name: 'Jiangsu', cn: '江苏', kind: 'province',
        cities: [
          { id: 'nanjing', name: 'Nanjing', cn: '南京', universities: [ { name: 'Nanjing University', cn: '南京大学' }, { name: 'Southeast University', cn: '东南大学' } ] },
          { id: 'suzhou', name: 'Suzhou', cn: '苏州', universities: [ { name: 'Soochow University', cn: '苏州大学' }, { name: 'Xi\'an Jiaotong-Liverpool University', cn: '西交利物浦大学' } ] },
        ],
      },
      {
        slug: 'zhejiang', name: 'Zhejiang', cn: '浙江', kind: 'province',
        cities: [
          { id: 'hangzhou', name: 'Hangzhou', cn: '杭州', universities: [ { name: 'Zhejiang University', cn: '浙江大学' } ] },
          { id: 'ningbo', name: 'Ningbo', cn: '宁波', universities: [ { name: 'Ningbo University', cn: '宁波大学' }, { name: 'University of Nottingham Ningbo China', cn: '宁波诺丁汉大学' } ] },
        ],
      },
      {
        slug: 'anhui', name: 'Anhui', cn: '安徽', kind: 'province',
        cities: [
          { id: 'hefei', name: 'Hefei', cn: '合肥', universities: [ { name: 'University of Science and Technology of China', cn: '中国科学技术大学' }, { name: 'Hefei University of Technology', cn: '合肥工业大学' } ] },
          { id: 'wuhu', name: 'Wuhu', cn: '芜湖', universities: [ { name: 'Anhui Normal University', cn: '安徽师范大学' } ] },
        ],
      },
      {
        slug: 'fujian', name: 'Fujian', cn: '福建', kind: 'province',
        cities: [
          { id: 'fuzhou', name: 'Fuzhou', cn: '福州', universities: [ { name: 'Fuzhou University', cn: '福州大学' } ] },
          { id: 'xiamen', name: 'Xiamen', cn: '厦门', universities: [ { name: 'Xiamen University', cn: '厦门大学' } ] },
        ],
      },
      {
        slug: 'jiangxi', name: 'Jiangxi', cn: '江西', kind: 'province',
        cities: [
          { id: 'nanchang', name: 'Nanchang', cn: '南昌', universities: [ { name: 'Nanchang University', cn: '南昌大学' } ] },
          { id: 'jingdezhen', name: 'Jingdezhen', cn: '景德镇', universities: [ { name: 'Jingdezhen Ceramic University', cn: '景德镇陶瓷大学' } ] },
        ],
      },
      {
        slug: 'shandong', name: 'Shandong', cn: '山东', kind: 'province',
        cities: [
          { id: 'jinan', name: 'Jinan', cn: '济南', universities: [ { name: 'Shandong University', cn: '山东大学' } ] },
          { id: 'qingdao', name: 'Qingdao', cn: '青岛', universities: [ { name: 'Ocean University of China', cn: '中国海洋大学' }, { name: 'China University of Petroleum (East China)', cn: '中国石油大学（华东）' } ] },
        ],
      },
    ],
  },
  {
    id: 'central',
    name: 'Central China',
    divisions: [
      {
        slug: 'henan', name: 'Henan', cn: '河南', kind: 'province',
        cities: [
          { id: 'zhengzhou', name: 'Zhengzhou', cn: '郑州', universities: [ { name: 'Zhengzhou University', cn: '郑州大学' } ] },
          { id: 'kaifeng', name: 'Kaifeng', cn: '开封', universities: [ { name: 'Henan University', cn: '河南大学' } ] },
        ],
      },
      {
        slug: 'hubei', name: 'Hubei', cn: '湖北', kind: 'province',
        cities: [
          { id: 'wuhan', name: 'Wuhan', cn: '武汉', universities: [ { name: 'Wuhan University', cn: '武汉大学' }, { name: 'Huazhong University of Science and Technology', cn: '华中科技大学' } ] },
          { id: 'yichang', name: 'Yichang', cn: '宜昌', universities: [ { name: 'China Three Gorges University', cn: '三峡大学' } ] },
        ],
      },
      {
        slug: 'hunan', name: 'Hunan', cn: '湖南', kind: 'province',
        cities: [
          { id: 'changsha', name: 'Changsha', cn: '长沙', universities: [ { name: 'Central South University', cn: '中南大学' }, { name: 'Hunan University', cn: '湖南大学' }, { name: 'National University of Defense Technology', cn: '国防科技大学' } ] },
          { id: 'xiangtan', name: 'Xiangtan', cn: '湘潭', universities: [ { name: 'Xiangtan University', cn: '湘潭大学' } ] },
        ],
      },
    ],
  },
  {
    id: 'south',
    name: 'South China',
    divisions: [
      {
        slug: 'guangdong', name: 'Guangdong', cn: '广东', kind: 'province',
        cities: [
          { id: 'guangzhou', name: 'Guangzhou', cn: '广州', universities: [ { name: 'Sun Yat-sen University', cn: '中山大学' }, { name: 'South China University of Technology', cn: '华南理工大学' } ] },
          { id: 'shenzhen', name: 'Shenzhen', cn: '深圳', universities: [ { name: 'Shenzhen University', cn: '深圳大学' }, { name: 'Southern University of Science and Technology', cn: '南方科技大学' } ] },
        ],
      },
      {
        slug: 'guangxi', name: 'Guangxi', cn: '广西', kind: 'autonomousRegion',
        cities: [
          { id: 'nanning', name: 'Nanning', cn: '南宁', universities: [ { name: 'Guangxi University', cn: '广西大学' } ] },
          { id: 'guilin', name: 'Guilin', cn: '桂林', universities: [ { name: 'Guangxi Normal University', cn: '广西师范大学' }, { name: 'Guilin University of Technology', cn: '桂林理工大学' } ] },
        ],
      },
      {
        slug: 'hainan', name: 'Hainan', cn: '海南', kind: 'province',
        cities: [
          { id: 'haikou', name: 'Haikou', cn: '海口', universities: [ { name: 'Hainan University', cn: '海南大学' } ] },
          { id: 'sanya', name: 'Sanya', cn: '三亚', universities: [ { name: 'Hainan Tropical Ocean University', cn: '海南热带海洋学院' } ] },
        ],
      },
    ],
  },
  {
    id: 'southwest',
    name: 'Southwest China',
    divisions: [
      {
        slug: 'chongqing', name: 'Chongqing', cn: '重庆', kind: 'municipality',
        cities: [
          { id: 'shapingba-district', name: 'Shapingba District', cn: '沙坪坝区', universities: [ { name: 'Chongqing University', cn: '重庆大学' }, { name: 'Southwest University', cn: '西南大学' } ] },
          { id: 'yuzhong-district', name: 'Yuzhong District', cn: '渝中区', universities: [ { name: 'Chongqing Medical University', cn: '重庆医科大学' } ] },
        ],
      },
      {
        slug: 'sichuan', name: 'Sichuan', cn: '四川', kind: 'province',
        cities: [
          { id: 'chengdu', name: 'Chengdu', cn: '成都', universities: [ { name: 'Sichuan University', cn: '四川大学' }, { name: 'University of Electronic Science and Technology of China', cn: '电子科技大学' }, { name: 'Southwest Jiaotong University', cn: '西南交通大学' } ] },
          { id: 'mianyang', name: 'Mianyang', cn: '绵阳', universities: [ { name: 'Southwest University of Science and Technology', cn: '西南科技大学' } ] },
        ],
      },
      {
        slug: 'guizhou', name: 'Guizhou', cn: '贵州', kind: 'province',
        cities: [
          { id: 'guiyang', name: 'Guiyang', cn: '贵阳', universities: [ { name: 'Guizhou University', cn: '贵州大学' } ] },
          { id: 'zunyi', name: 'Zunyi', cn: '遵义', universities: [ { name: 'Zunyi Medical University', cn: '遵义医科大学' } ] },
        ],
      },
      {
        slug: 'yunnan', name: 'Yunnan', cn: '云南', kind: 'province',
        cities: [
          { id: 'kunming', name: 'Kunming', cn: '昆明', universities: [ { name: 'Yunnan University', cn: '云南大学' }, { name: 'Kunming University of Science and Technology', cn: '昆明理工大学' } ] },
          { id: 'dali', name: 'Dali', cn: '大理', universities: [ { name: 'Dali University', cn: '大理大学' } ] },
        ],
      },
      {
        slug: 'tibet', name: 'Tibet', cn: '西藏', kind: 'autonomousRegion',
        cities: [
          { id: 'lhasa', name: 'Lhasa', cn: '拉萨', universities: [ { name: 'Tibet University', cn: '西藏大学' } ] },
          { id: 'nyingchi', name: 'Nyingchi', cn: '林芝', universities: [ { name: 'Tibet Agricultural and Animal Husbandry University', cn: '西藏农牧学院' } ] },
        ],
      },
    ],
  },
  {
    id: 'northwest',
    name: 'Northwest China',
    divisions: [
      {
        slug: 'shaanxi', name: 'Shaanxi', cn: '陕西', kind: 'province',
        cities: [
          { id: 'xian', name: 'Xi\'an', cn: '西安', universities: [ { name: 'Xi\'an Jiaotong University', cn: '西安交通大学' }, { name: 'Northwestern Polytechnical University', cn: '西北工业大学' }, { name: 'Xidian University', cn: '西安电子科技大学' } ] },
          { id: 'yangling', name: 'Yangling', cn: '杨凌', universities: [ { name: 'Northwest A&F University', cn: '西北农林科技大学' } ] },
        ],
      },
      {
        slug: 'gansu', name: 'Gansu', cn: '甘肃', kind: 'province',
        cities: [
          { id: 'lanzhou', name: 'Lanzhou', cn: '兰州', universities: [ { name: 'Lanzhou University', cn: '兰州大学' } ] },
          { id: 'tianshui', name: 'Tianshui', cn: '天水', universities: [ { name: 'Tianshui Normal University', cn: '天水师范学院' } ] },
        ],
      },
      {
        slug: 'qinghai', name: 'Qinghai', cn: '青海', kind: 'province',
        cities: [
          { id: 'xining', name: 'Xining', cn: '西宁', universities: [ { name: 'Qinghai University', cn: '青海大学' } ] },
          { id: 'golmud', name: 'Golmud', cn: '格尔木', universities: [ { name: 'Qinghai University (Golmud campus)', cn: '青海大学格尔木校区' } ] },
        ],
      },
      {
        slug: 'ningxia', name: 'Ningxia', cn: '宁夏', kind: 'autonomousRegion',
        cities: [
          { id: 'yinchuan', name: 'Yinchuan', cn: '银川', universities: [ { name: 'Ningxia University', cn: '宁夏大学' } ] },
          { id: 'zhongwei', name: 'Zhongwei', cn: '中卫', universities: [ { name: 'Ningxia University (Zhongwei campus)', cn: '宁夏大学中卫校区' } ] },
        ],
      },
      {
        slug: 'xinjiang', name: 'Xinjiang', cn: '新疆', kind: 'autonomousRegion',
        cities: [
          { id: 'urumqi', name: 'Ürümqi', cn: '乌鲁木齐', universities: [ { name: 'Xinjiang University', cn: '新疆大学' } ] },
          { id: 'kashgar', name: 'Kashgar', cn: '喀什', universities: [ { name: 'Kashgar University', cn: '喀什大学' } ] },
        ],
      },
    ],
  },
]

export const CHINA_DIVISION_COUNT = CHINA_REGIONS.reduce((n, r) => n + r.divisions.length, 0)
