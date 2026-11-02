/**
 * 一键让演示学生「陈嘉禾」把作业做完（用于演示"提交作业 / 上传附件 / 待批改"全流程）。
 *
 *   node scripts/complete-homework.mjs                 # 提交未提交的作业（已提交则跳过）
 *   node scripts/complete-homework.mjs --force         # 重新提交（覆盖未批改的答案）
 *   API_BASE=http://localhost:8081 node scripts/complete-homework.mjs
 *
 * 说明：数据库被重置（删除 db.json）后可再次运行，恢复"作业已完成"的演示状态。
 */
import fs from 'node:fs';
import path from 'node:path';

const API_BASE = (process.env.API_BASE || 'http://localhost:8080').replace(/\/$/, '');
const USERNAME = process.env.STUDENT_USERNAME || 'chen';
const PASSWORD = process.env.STUDENT_PASSWORD || '123456';
const FORCE = process.argv.includes('--force');

/* ------------------------------- 作业答案 ------------------------------- */

const ANSWERS_101 = String.raw`【一、三处例句】

1. 通感（第 4 段）：“微风过处，送来缕缕清香，仿佛远处高楼上渺茫的歌声似的。”——用听觉的“歌声”来写嗅觉的“清香”，两种感官彼此打通。
2. 比喻（第 5 段）：“叶子出水很高，像亭亭的舞女的裙。”——以舞女旋转时舒展的裙裾，状写荷叶挺立、铺展的姿态。
3. 叠词（第 5、6 段）：“曲曲折折的荷塘上面，弥望的是田田的叶子”“月光如流水一般，静静地泻在这一片叶子和花上。”——“曲曲折折”“田田”“静静”叠字连用。

【二、赏析：为什么是“渺茫的歌声”】

朱自清不直说“荷香”，先说“缕缕”，香便有了丝缕的形状，能被风看得见似的一缕缕送来；又不直说“清香”，而以“远处高楼上渺茫的歌声”作比，香于是又有了声音。这一步跨得极妙：歌声在远处、在高处，隔着距离传来，便不再清晰，只剩下若有若无的断续，正合荷香的淡、远、幽。若换成“浓香扑鼻”，嗅觉立刻变得确定而逼人，与月下荷塘的朦胧气场全然不合；“渺茫”二字又自带一层怅惘，香越是抓不住，人越是凝神去听、去闻，读者也就跟着慢下来。

更值得注意的是它的呼应关系：第 4 段香与歌声相通，第 5 段光与流水相融（“月光如流水一般，静静地泻在……”），第 6 段则写到“光与影有着和谐的旋律，如梵婀玲上奏着的名曲”，视觉又一次通向听觉。三处通感层层递进，把荷塘写成一支由清香、月色、树影共同演奏的曲子。而这一切都发生在“这几天心里颇不宁静”之后——通感造成的朦胧与不确定，正是作者此刻心境的写照：淡淡的喜悦，淡淡的哀愁，想要超脱又终究回到尘世。写景的技法在这里不是炫技，而是情绪的形状。`;

const ANSWERS_102 = String.raw`【一、《短歌行》】
对酒当歌，人生几何！譬如朝露，去日苦多。
慨当以慷，忧思难忘。何以解忧？唯有杜康。
青青子衿，悠悠我心。但为君故，沉吟至今。
呦呦鹿鸣，食野之苹。我有嘉宾，鼓瑟吹笙。
明明如月，何时可掇？忧从中来，不可断绝。
越陌度阡，枉用相存。契阔谈䜩，心念旧恩。
月明星稀，乌鹊南飞。绕树三匝，何枝可依？
山不厌高，海不厌深。周公吐哺，天下归心。

【二、《归园田居（其一）》前八句】
少无适俗韵，性本爱丘山。误落尘网中，一去三十年。
羁鸟恋旧林，池鱼思故渊。开荒南野际，守拙归园田。

【三、《念奴娇·赤壁怀古》下阕】
遥想公瑾当年，小乔初嫁了，雄姿英发。羽扇纶巾，谈笑间，樯橹灰飞烟灭。故国神游，多情应笑我，早生华发。人生如梦，一尊还酹江月。`;

const ANSWERS_104 = String.raw`我身边的小人物

巷口那棵香樟底下，常年支着一个修鞋摊。摊主姓周，我们都叫他老周——这自然是化名，二十几年了，没人认真打听过他的本名。

摊子极简：一张矮脚木桌，一把铁脚凳，一只装钉子的旧饼干铁盒，盒盖上用红漆写着“钉、线、胶”。桌角有一块磨得发亮的油石，油石旁边永远搭着一条看不出原色的抹布。他不吆喝，只在樟树干上挂块硬纸牌：“修鞋、换底、缝包。”

我第一次去找他，是鞋跟断在放学路上。他没急着接，先翻过来看了一面，又看另一面，用拇指按了按鞋底：“不是跟断了，是里面那层海绵让汗泡烂了。光换跟，一个月还得坏。”说完从铁盒里挑出一片旧海绵，比着鞋里剪成月牙，边缘剪得比鞋帮略窄一点，“窄一点才不硌脚。”针脚走完，他把鞋举起来对着光看，又用抹布沿着缝线抹了一圈，才递给我：“回去别急着穿，搁一晚，胶要醒。”

后来我留意到，他修鞋时有个习惯：先把鞋跟对着自己鼻子，闻一下。有人笑他，他说：“鞋里藏着人的日子。汗味重的，多半干活站着；香水味盖不住的，多半天天走路。”他不识字，却能凭一双鞋说出这人的营生，说完也不多问，低头把线抽紧。

冬天最冷那阵，他在摊边放了个暖水瓶，谁路过都能倒一杯热水。有流浪猫蹲在凳子下，他把饼干盒里的碎皮子垫在纸箱底，说“皮子隔潮”。城管来劝过两次，他把摊子往后挪了半米，挪完仍旧坐在那儿，像香樟自己长出来的一根枝。

我常想，我们谈理想、谈远方，谈那些会被写进新闻的事；而城市的秩序其实是由这样的人一寸一寸缝起来的。他们不制造秩序，只是不让自己那一小块地方松线：鞋跟钉牢，缝线抹平，热水留给别人。所谓勤勉、诚实与体面，在这种日复一日的手艺里，比任何一句口号都更像证据。他们不站在光里，却让光落下来时不至于跌碎。`;

const HANDWRITING_SVG = String.raw`<svg xmlns="http://www.w3.org/2000/svg" width="760" height="960" viewBox="0 0 760 960">
<rect width="760" height="960" fill="#fdfcf7"/>
<line x1="70" y1="96" x2="70" y2="900" stroke="#f0c9c9" stroke-width="1.5"/>
<line x1="70" y1="150" x2="690" y2="150" stroke="#e3e7ef" stroke-width="1"/><line x1="70" y1="194" x2="690" y2="194" stroke="#e3e7ef" stroke-width="1"/><line x1="70" y1="238" x2="690" y2="238" stroke="#e3e7ef" stroke-width="1"/><line x1="70" y1="282" x2="690" y2="282" stroke="#e3e7ef" stroke-width="1"/><line x1="70" y1="326" x2="690" y2="326" stroke="#e3e7ef" stroke-width="1"/><line x1="70" y1="370" x2="690" y2="370" stroke="#e3e7ef" stroke-width="1"/><line x1="70" y1="414" x2="690" y2="414" stroke="#e3e7ef" stroke-width="1"/><line x1="70" y1="458" x2="690" y2="458" stroke="#e3e7ef" stroke-width="1"/><line x1="70" y1="502" x2="690" y2="502" stroke="#e3e7ef" stroke-width="1"/><line x1="70" y1="546" x2="690" y2="546" stroke="#e3e7ef" stroke-width="1"/><line x1="70" y1="590" x2="690" y2="590" stroke="#e3e7ef" stroke-width="1"/><line x1="70" y1="634" x2="690" y2="634" stroke="#e3e7ef" stroke-width="1"/><line x1="70" y1="678" x2="690" y2="678" stroke="#e3e7ef" stroke-width="1"/><line x1="70" y1="722" x2="690" y2="722" stroke="#e3e7ef" stroke-width="1"/><line x1="70" y1="766" x2="690" y2="766" stroke="#e3e7ef" stroke-width="1"/><line x1="70" y1="810" x2="690" y2="810" stroke="#e3e7ef" stroke-width="1"/><line x1="70" y1="854" x2="690" y2="854" stroke="#e3e7ef" stroke-width="1"/><line x1="70" y1="898" x2="690" y2="898" stroke="#e3e7ef" stroke-width="1"/>
<text x="86" y="142" font-size="25" fill="#22314a">《荷塘月色》赏析 · 手写稿</text><text x="86" y="230" font-size="25" fill="#22314a">朱自清不直说“荷香”，先说“缕缕”，香便有了丝缕的形状，</text><text x="86" y="274" font-size="25" fill="#22314a">能被风看得见似的一缕缕送来；又不直说“清香”，而以“远处高楼</text><text x="86" y="318" font-size="25" fill="#22314a">上渺茫的歌声”作比，香于是又有了声音。</text><text x="86" y="406" font-size="25" fill="#22314a">歌声在远处、在高处，隔着距离传来，便不再清晰，只剩下</text><text x="86" y="450" font-size="25" fill="#22314a">若有若无的断续，正合荷香的淡、远、幽。若换成“浓香扑鼻”，</text><text x="86" y="494" font-size="25" fill="#22314a">嗅觉立刻变得确定而逼人，与月下荷塘的朦胧气场全然不合；</text><text x="86" y="538" font-size="25" fill="#22314a">“渺茫”二字又自带一层怅惘——香越是抓不住，人越是凝神</text><text x="86" y="582" font-size="25" fill="#22314a">去听、去闻，读者也就跟着慢下来。</text><text x="86" y="670" font-size="25" fill="#22314a">三处通感层层递进：香与歌声相通，光与水相融，光与影又</text><text x="86" y="714" font-size="25" fill="#22314a">成了梵婀玲上的名曲。写景的技法在这里不是炫技，而是</text><text x="86" y="758" font-size="25" fill="#22314a">情绪的形状。</text><text x="86" y="846" font-size="25" fill="#22314a">—— 陈嘉禾  20240107</text>
<text x="380" y="940" text-anchor="middle" font-family="sans-serif" font-size="15" fill="#98a1b3">手写稿扫描件 · 作业附件示例</text>
</svg>`;

/* ------------------------------- 工具函数 ------------------------------- */

let token = '';

async function api(pathname, { method = 'GET', body, form } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body) headers['Content-Type'] = 'application/json';
  const res = await fetch(API_BASE + pathname, {
    method,
    headers,
    body: form || (body ? JSON.stringify(body) : undefined)
  });
  const text = await res.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }
  if (!res.ok) {
    throw new Error(`${method} ${pathname} -> ${res.status} ${data?.message || text}`);
  }
  return data;
}

async function login() {
  const res = await api('/api/auth/login', { method: 'POST', body: { username: USERNAME, password: PASSWORD } });
  token = res.token;
  console.log(`✓ 已登录：${res.user.name}（${res.user.role}，学号 ${res.user.studentNo}）`);
}

async function uploadHandwriting() {
  const fd = new FormData();
  fd.append('files', new Blob([HANDWRITING_SVG], { type: 'image/svg+xml' }), '荷塘月色赏析-手写稿.svg');
  const [file] = await api('/api/files', { method: 'POST', form: fd });
  console.log(`✓ 已上传附件：${file.name}（${file.size} 字节）`);
  return file.id;
}

/* ------------------------------- 主流程 ------------------------------- */

async function main() {
  await login();

  const assignments = await api('/api/assignments');
  const byId = new Map(assignments.map((a) => [a.id, a]));

  const tasks = [
    { id: 101, title: '《荷塘月色》第 4-6 段赏析', content: ANSWERS_101, withAttachment: true },
    { id: 102, title: '古诗文默写 · 必修上册第三单元', content: ANSWERS_102, withAttachment: false },
    { id: 104, title: '周末随笔：我身边的小人物', content: ANSWERS_104, withAttachment: false }
  ];

  let attachmentId = null;
  let submitted = 0;
  let skipped = 0;

  for (const task of tasks) {
    const assignment = byId.get(task.id);
    if (!assignment) {
      console.log(`- 作业 ${task.id} 对当前学生不可见（未发布或不在该班级），跳过`);
      continue;
    }
    if (assignment.mySubmission && !FORCE) {
      console.log(`- ${assignment.title}：已提交（${assignment.mySubmission.status}），跳过`);
      skipped += 1;
      continue;
    }
    const attachments = [];
    if (task.withAttachment) {
      attachmentId = attachmentId || (await uploadHandwriting());
      attachments.push(attachmentId);
    }
    const result = await api(`/api/assignments/${task.id}/submissions`, {
      method: 'POST',
      body: { content: task.content, attachments }
    });
    console.log(
      `✓ 已提交 ${assignment.title}：${result.content.length} 字` +
        (attachments.length ? `，含 ${attachments.length} 个附件` : '') +
        (result.late ? '（逾期提交）' : '')
    );
    submitted += 1;
  }

  const pending = byId.size
    ? [...byId.values()].filter((a) => !a.mySubmission && a.id !== 103)
    : [];
  console.log(`\n完成：新提交 ${submitted} 份，跳过 ${skipped} 份。`);
  if (pending.length) {
    console.log('仍未提交：' + pending.map((a) => a.title).join('、'));
  }
  console.log('提示：作业 103 现代文阅读训练（一）已逾期且「不允许逾期补交」，用于演示提交拦截，故不在自动提交范围内。');
}

main().catch((err) => {
  console.error('✗ 执行失败：', err.message);
  process.exit(1);
});
