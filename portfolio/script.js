/* ============================
   PORTFOLIO JAVASCRIPT
   ============================ */

// ── Navbar scroll effect ──────────────────────────────────────
const navbar = document.getElementById('navbar');
window.addEventListener('scroll', () => {
  if (window.scrollY > 50) {
    navbar.classList.add('scrolled');
  } else {
    navbar.classList.remove('scrolled');
  }
}, { passive: true });

// ── Hamburger Menu ────────────────────────────────────────────
const hamburger = document.getElementById('hamburger');
const navLinks = document.getElementById('nav-links');

hamburger.addEventListener('click', () => {
  navLinks.classList.toggle('open');
  hamburger.classList.toggle('active');
});

// Close menu when clicking a link
navLinks.querySelectorAll('.nav-link').forEach(link => {
  link.addEventListener('click', () => {
    navLinks.classList.remove('open');
    hamburger.classList.remove('active');
  });
});

// ── Particle System ───────────────────────────────────────────
function createParticles() {
  const container = document.getElementById('particles');
  if (!container) return;

  const colors = ['#6366f1', '#8b5cf6', '#06b6d4', '#10b981', '#f59e0b'];
  const count = 30;

  for (let i = 0; i < count; i++) {
    const particle = document.createElement('div');
    particle.className = 'particle';

    const size = Math.random() * 3 + 1;
    const x = Math.random() * 100;
    const duration = Math.random() * 15 + 10;
    const delay = Math.random() * 15;
    const color = colors[Math.floor(Math.random() * colors.length)];

    particle.style.cssText = `
      left: ${x}%;
      width: ${size}px;
      height: ${size}px;
      background: ${color};
      animation-duration: ${duration}s;
      animation-delay: ${delay}s;
      box-shadow: 0 0 ${size * 3}px ${color};
    `;

    container.appendChild(particle);
  }
}

createParticles();

// ── Scroll Reveal ─────────────────────────────────────────────
function setupReveal() {
  const elements = document.querySelectorAll([
    '.about-card',
    '.arch-node',
    '.infra-node',
    '.flow-card',
    '.stack-category',
    '.trouble-card',
    '.k8s-step',
    '.result-card',
    '.flow-step',
  ].join(', '));

  elements.forEach((el, index) => {
    el.classList.add('reveal');

    // Stagger sibling elements
    const siblings = Array.from(el.parentElement.children);
    const siblingIndex = siblings.indexOf(el);
    if (siblingIndex > 0) {
      const delays = ['reveal-delay-1', 'reveal-delay-2', 'reveal-delay-3', 'reveal-delay-4'];
      const delayClass = delays[Math.min(siblingIndex - 1, delays.length - 1)];
      el.classList.add(delayClass);
    }
  });

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible');
      }
    });
  }, {
    threshold: 0.1,
    rootMargin: '0px 0px -50px 0px'
  });

  elements.forEach(el => observer.observe(el));
}

setupReveal();

// ── Animated Number Counter ───────────────────────────────────
function animateCounter(el, target, duration = 1500) {
  const start = 0;
  const startTime = performance.now();

  function update(currentTime) {
    const elapsed = currentTime - startTime;
    const progress = Math.min(elapsed / duration, 1);

    // Ease out cubic
    const eased = 1 - Math.pow(1 - progress, 3);
    const current = Math.round(eased * target);

    el.textContent = current;

    if (progress < 1) {
      requestAnimationFrame(update);
    } else {
      el.textContent = target;
    }
  }

  requestAnimationFrame(update);
}

function setupCounters() {
  const counters = document.querySelectorAll('[data-target]');
  
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const target = parseInt(entry.target.dataset.target, 10);
        animateCounter(entry.target, target);
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.5 });

  counters.forEach(counter => observer.observe(counter));
}

setupCounters();

// ── Tab System ────────────────────────────────────────────────
function setupTabs() {
  const tabBtns = document.querySelectorAll('.tab-btn');
  const tabContents = document.querySelectorAll('.tab-content');

  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetTab = btn.dataset.tab;

      // Deactivate all
      tabBtns.forEach(b => b.classList.remove('active'));
      tabContents.forEach(c => c.classList.remove('active'));

      // Activate target
      btn.classList.add('active');
      const content = document.getElementById(`tab-content-${targetTab}`);
      if (content) {
        content.classList.add('active');
        
        // Re-trigger reveal animations in newly visible tab
        const revealEls = content.querySelectorAll('.reveal');
        revealEls.forEach(el => {
          el.classList.remove('visible');
          setTimeout(() => el.classList.add('visible'), 50);
        });
      }
    });
  });
}

setupTabs();

// ── Active Nav Link on Scroll ─────────────────────────────────
function setupActiveNav() {
  const sections = document.querySelectorAll('section[id]');
  const navLinksAll = document.querySelectorAll('.nav-link');

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        navLinksAll.forEach(link => {
          link.style.color = '';
          if (link.getAttribute('href') === `#${entry.target.id}`) {
            link.style.color = 'var(--accent-3)';
          }
        });
      }
    });
  }, {
    rootMargin: '-40% 0px -55% 0px'
  });

  sections.forEach(section => observer.observe(section));
}

setupActiveNav();

// ── Hover Glow Effect on Architecture nodes ───────────────────
document.querySelectorAll('.arch-node, .infra-node').forEach(node => {
  node.addEventListener('mousemove', (e) => {
    const rect = node.getBoundingClientRect();
    const x = ((e.clientX - rect.left) / rect.width) * 100;
    const y = ((e.clientY - rect.top) / rect.height) * 100;
    node.style.setProperty('--mouse-x', `${x}%`);
    node.style.setProperty('--mouse-y', `${y}%`);
  });
});

// ── Smooth typing effect for hero subtitle ────────────────────
function setupTypingEffect() {
  const desc = document.querySelector('.hero-desc');
  if (!desc) return;

  // Just add a subtle animated underline to tech terms
  const techTerms = ['Spring Boot', 'Kubernetes', 'Kafka', 'Redis', 'Elasticsearch'];
  let html = desc.innerHTML;
  
  techTerms.forEach(term => {
    html = html.replace(
      new RegExp(term, 'g'),
      `<span class="tech-highlight">${term}</span>`
    );
  });
  
  desc.innerHTML = html;
}

// Add tech highlight styles
const style = document.createElement('style');
style.textContent = `
  .tech-highlight {
    color: var(--accent-3);
    font-weight: 600;
    position: relative;
  }
  .tech-highlight::after {
    content: '';
    position: absolute;
    bottom: -1px;
    left: 0;
    right: 0;
    height: 1px;
    background: var(--accent-3);
    opacity: 0.4;
  }
`;
document.head.appendChild(style);

setupTypingEffect();

// ── Trouble card expand (mobile) ──────────────────────────────
document.querySelectorAll('.trouble-card').forEach(card => {
  card.addEventListener('click', (e) => {
    if (window.innerWidth > 768) return;
    if (e.target.closest('code') || e.target.closest('pre')) return;
    card.classList.toggle('expanded');
  });
});

// ── Architecture nodes connection animation ───────────────────
function animateConnectionLines() {
  const nodes = document.querySelectorAll('.arch-node');
  nodes.forEach((node, i) => {
    node.style.animationDelay = `${i * 0.1}s`;
  });
}
animateConnectionLines();

// ── Scroll to top on logo click ───────────────────────────────
document.querySelector('.nav-logo')?.addEventListener('click', (e) => {
  e.preventDefault();
  window.scrollTo({ top: 0, behavior: 'smooth' });
});

// ── Journey steps animation ───────────────────────────────────
function setupJourneyAnimation() {
  const journeySteps = document.querySelectorAll('.journey-step');
  
  const observer = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting) {
      journeySteps.forEach((step, i) => {
        setTimeout(() => {
          step.style.opacity = '0';
          step.style.transform = 'scale(0.8)';
          step.style.transition = 'all 0.4s ease';
          
          setTimeout(() => {
            step.style.opacity = '1';
            step.style.transform = 'scale(1)';
          }, 50);
        }, i * 150);
      });
      observer.disconnect();
    }
  }, { threshold: 0.5 });

  const journeyContainer = document.querySelector('.result-journey');
  if (journeyContainer) observer.observe(journeyContainer);
}

setupJourneyAnimation();

// ── Stack badge hover ripple ──────────────────────────────────
document.querySelectorAll('.stack-badge').forEach(badge => {
  badge.addEventListener('click', (e) => {
    const ripple = document.createElement('span');
    ripple.style.cssText = `
      position: absolute;
      border-radius: 50%;
      background: rgba(255,255,255,0.3);
      transform: scale(0);
      animation: ripple 0.6s linear;
      pointer-events: none;
      width: 60px;
      height: 60px;
      left: 50%;
      top: 50%;
      margin: -30px 0 0 -30px;
    `;

    const style = document.createElement('style');
    style.textContent = `@keyframes ripple { to { transform: scale(4); opacity: 0; } }`;
    document.head.appendChild(style);

    badge.style.position = 'relative';
    badge.style.overflow = 'hidden';
    badge.appendChild(ripple);
    setTimeout(() => ripple.remove(), 600);
  });
});

// ── Section background parallax ───────────────────────────────
if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
  window.addEventListener('scroll', () => {
    const scrollY = window.scrollY;
    const glow1 = document.querySelector('.glow-1');
    const glow2 = document.querySelector('.glow-2');
    
    if (glow1) glow1.style.transform = `translateY(${scrollY * 0.1}px)`;
    if (glow2) glow2.style.transform = `translateY(${-scrollY * 0.05}px)`;
  }, { passive: true });
}

console.log('🚀 Portfolio loaded. Built with Spring Boot + Kubernetes + ❤️');
