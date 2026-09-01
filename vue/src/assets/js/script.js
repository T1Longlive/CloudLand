/**
 * 前台首页导航交互（仅 NavBar 首页模式使用）：
 * - 移动端菜单按钮开合
 * - 页面滚动时 header 加 .active 类（透明 → 品牌绿）
 * 返回清理函数，组件销毁时调用，避免滚动监听泄漏。
 */
export function myFunction() {
  const menu = document.querySelector('#menu-btn');
  const navbar = document.querySelector('.header .nav');
  const header = document.querySelector('.header');
  if (!menu || !navbar || !header) {
    return () => null;
  }

  const onClick = () => {
    menu.classList.toggle('fa-times');
    navbar.classList.toggle('active');
  };
  const onScroll = () => {
    menu.classList.remove('fa-times');
    navbar.classList.remove('active');
    if (window.scrollY > 0) {
      header.classList.add('active');
    } else {
      header.classList.remove('active');
    }
  };

  menu.addEventListener('click', onClick);
  window.addEventListener('scroll', onScroll);

  return () => {
    menu.removeEventListener('click', onClick);
    window.removeEventListener('scroll', onScroll);
  };
}
