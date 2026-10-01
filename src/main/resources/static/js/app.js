/**
 * This is main script file that contains JS code.
 */
(function ($) {
    // Global jQuery AJAX CSRF configuration
    $(document).ajaxSend(function(event, xhr, options) {
        const token = $('#csrf-token-holder').data('csrf-token');
        const header = $('#csrf-token-holder').data('csrf-header');
        if (token && header) {
            xhr.setRequestHeader(header, token);
        }
    });

    // Main Object
    var RESHOP = {};

    
    

    

    // Predefined variables
    var
        $filterGridWrapper = $('.filter__grid-wrapper'),
        $collectionOfFilterBtn = $('.filter__btn'),
        $primarySlider = $('#hero-slider'),
        $testimonialSlider = $('#testimonial-slider'),
        $collectionaClickScroll = $('[data-click-scroll]'),
        $collectionProductSlider = $('.product-slider'),
        $collectionTabSlider = $('.tab-slider'),
        $collectionInputCounter = $('.input-counter'),
        $collectionCartModalLink = $('[data-modal="modal"]'),
        $defaultAddressCheckbox = $('#get-address'),
        $collectionFormBill = $('[data-bill]'),
        $productDetailElement = $('#pd-o-initiate'),
        $productDetailElementThumbnail = $('#pd-o-thumbnail'),
        $modalProductDetailElement = $('#js-product-detail-modal'),
        $modalProductDetailElementThumbnail = $('#js-product-detail-modal-thumbnail'),
        $shopCategoryToggleSpan = $('.shop-w__category-list .has-list > .js-shop-category-span'),// Recursive
        $shopGridBtn = $('.js-shop-grid-target'),
        $shopListBtn = $('.js-shop-list-target'),
        $shopPerspectiveRow = $('.shop-p__collection > div'),
        $shopFilterBtn = $('.js-shop-filter-target');



    // Bind Scroll Up to all pages
    RESHOP.initScrollUp = function() {
        $.scrollUp({
            scrollName: 'topScroll',
            scrollText: '<i class="fas fa-long-arrow-alt-up"></i>',
            easingType: 'linear',
            scrollSpeed: 900,
            animation: 'fade',
            zIndex: 100
        });
    };

    RESHOP.initScrollSpy = function() {
        var $bodyScrollSpy = $('#js-scrollspy-trigger');
        if ($bodyScrollSpy.length) {
            $bodyScrollSpy.scrollspy({
                target: '#init-scrollspy'
            });
        }
    };

    RESHOP.onClickScroll = function() {
        $collectionaClickScroll.on('click', function (e) {
            // prevent default behavior means page doesn't move or show up id's on browser status-bar
            e.preventDefault();
            // Get Target
            var target = $(this).data('click-scroll');
            // check if anchor has hash
            if ($(target).length) {
                $('html').animate({
                    // .offset() is jQuery function and it returns jQuery object which
                    // has top, left, bottom property and returns total distance from the html container
                    scrollTop: $(target).offset().top
                }, 1000, function () {
                });
            }
        });
    };

    // Bind Tooltip to all pages
    RESHOP.initTooltip = function() {

        $('[data-tooltip="tooltip"]').tooltip({
            // The default value for trigger is 'hover focus',
            // thus the tooltip stay visible after a button is clicked,
            // until another button is clicked, because the button is focused.
            trigger : 'hover'
        });
    };

    // Bind Modals
    RESHOP.initModal = function() {
        // Check if these anchors are on page
        if ($collectionCartModalLink.length) {
            $collectionCartModalLink.on('click',function () {
                var getElemId = $(this).data('modal-id');
                $(getElemId).modal({
                    backdrop: 'static',
                    keyboard: false,
                    show:true
                });


            });
        }

    };

    // Default Billing Address
    RESHOP.defaultAddressCheckbox = function() {
        if ($defaultAddressCheckbox.length) {
            $defaultAddressCheckbox.change(function () {
                if (this.checked) {
                    $collectionFormBill.prop("disabled", true);
                    $('#make-default-address').prop("checked", false);
                } else {
                    $collectionFormBill.prop("disabled", false);
                }
            });

        }
    };





    RESHOP.reshopNavigation = function() {
        $('#navigation').shopNav();
        $('#navigation1').shopNav();
        $('#navigation2').shopNav();
        $('#navigation3').shopNav();
    };

    RESHOP.onTabActiveRefreshSlider = function() {
        // When showing a new tab, the events fire.
        // Specificity = 2
        $('.tab-list [data-toggle="tab"]').on('shown.bs.tab', function (e) {
            // Get the current click id of tab
            var currentID = $(e.target).attr('href');
            // Trigger refresh `event` to current active `tab`
            $(currentID + '.active').find('.tab-slider').trigger('refresh.owl.carousel');
        });
    };

    // Bind all sliders into the page
    RESHOP.primarySlider = function() {
        if ($primarySlider.length) {
            $primarySlider.owlCarousel({
                items: 1,
                autoplayTimeout: 8000,
                loop: true,
                margin: -1,
                dots: false,
                smartSpeed: 1500,
                rewind: false, // Go backwards when the boundary has reached
                nav: false,
                responsive: {
                    992: {
                        dots: true
                    }
                }
            });
        }
    };

    // Bind all sliders into the page
    RESHOP.productSlider = function() {
        // 0 is falsy value, 1 is truthy
        if ($collectionProductSlider.length) {
            $collectionProductSlider.on('initialize.owl.carousel', function () {
                $(this).closest('.slider-fouc').removeAttr('class');
            }).each(function () {
                var thisInstance = $(this);
                var itemPerLine = thisInstance.data('item');
                thisInstance.owlCarousel({
                    autoplay: false,
                    loop: false,
                    dots: false,
                    rewind: true,
                    smartSpeed: 1500,
                    nav: true,
                    navElement: 'div',
                    navClass: ['p-prev', 'p-next'],
                    navText: ['<i class="fas fa-long-arrow-alt-left"></i>', '<i class="fas fa-long-arrow-alt-right"></i>'],
                    responsive: {
                        0: {
                            items: 1
                        },
                        768: {
                            items: itemPerLine - 2
                        },
                        991: {
                            items: itemPerLine - 1
                        },
                        1200: {
                            items: itemPerLine
                        }
                    }
                });
            });
        }
    };


    // Bind all sliders into the page
    RESHOP.tabSlider = function() {
        if ($collectionTabSlider.length) {
            $collectionTabSlider.on('initialize.owl.carousel', function () {
                $(this).closest('.slider-fouc').removeAttr('class');
            }).each(function () {
                var thisInstance = $(this);
                var itemPerLine = thisInstance.data('item');
                thisInstance.owlCarousel({
                    autoplay: false,
                    loop: false,
                    dots: false,
                    rewind: true,
                    smartSpeed: 1500,
                    nav: true,
                    navElement: 'div',
                    navClass: ['t-prev', 't-next'],
                    navText: ['<i class="fas fa-long-arrow-alt-left"></i>', '<i class="fas fa-long-arrow-alt-right"></i>'],
                    responsive: {
                        0: {
                            items: 1
                        },
                        768: {
                            items: itemPerLine - 2
                        },
                        991: {
                            items: itemPerLine - 1
                        },
                        1200: {
                            items: itemPerLine
                        }
                    }
                });
            });
        }
    };

    // Bind Brand slider
    RESHOP.brandSlider = function() {
        var $brandSlider = $('#brand-slider');
        // Check if brand slider on the page
        if ($brandSlider.length) {
            var itemPerLine = $brandSlider.data('item');
            $brandSlider.on('initialize.owl.carousel', function () {
                $(this).closest('.slider-fouc').removeAttr('class');
            }).owlCarousel({
                autoplay: false,
                loop: false,
                dots: false,
                rewind: true,
                nav: true,
                navElement: 'div',
                navClass: ['b-prev', 'b-next'],
                navText: ['<i class="fas fa-angle-left"></i>', '<i class="fas fa-angle-right"></i>'],
                responsive: {
                    0: {
                        items: 1
                    },
                    768: {
                        items: 3,
                    },
                    991: {
                        items: itemPerLine
                    },
                    1200: {
                        items: itemPerLine
                    }
                }

            });
        }
    };

    // Testimonial Slider
    RESHOP.testimonialSlider = function() {
        // Check if Testimonial-Slider on the page
        if ($testimonialSlider.length) {
            $testimonialSlider.on('initialize.owl.carousel', function () {
                $(this).closest('.slider-fouc').removeAttr('class');
            }).owlCarousel({
                items:1,
                autoplay: false,
                loop: false,
                dots: true,
                rewind: false,
                smartSpeed: 1500,
                nav: false
            });
        }
    };
    // Remove Class from body element
    RESHOP.appConfiguration = function() {
        $('body').removeAttr('class');
        $('.preloader').removeClass('is-active');
    };





	// Input Counter
	RESHOP.initInputCounter = function() {
	    // Quét lại toàn bộ DOM để tìm các thẻ input-counter mới sinh ra
	    var $dynamicInputCounter = $('.input-counter'); 
	    
	    if ($dynamicInputCounter.length) {
	        // Gỡ các sự kiện click cũ trước khi gắn mới để không bị nhân đôi sự kiện
	        $dynamicInputCounter.find('.input-counter__plus, .input-counter__minus').off('click');
	        $dynamicInputCounter.find('input').off('change');

	        // Attach Click event to plus button
	        $dynamicInputCounter.find('.input-counter__plus').on('click',function () {
	            var $input = $(this).parent().find('input');
	            var count = parseInt($input.val()) + 1;
	            $input.val(count).change();
	        });
	        // Attach Click event to minus button
	        $dynamicInputCounter.find('.input-counter__minus').on('click',function () {
	            var $input = $(this).parent().find('input');
	            var count = parseInt($input.val()) - 1;
	            $input.val(count).change();
	        });
	        // Fires when the value of the element is changed
	        $dynamicInputCounter.find('input').change(function () {
	            var $this = $(this);
	            var min = $this.data('min');
	            var max = $this.data('max');
	            var val = parseInt($this.val());
	            if (!val) { val = 1; }
	            val = Math.min(val,max);
	            val = Math.max(val,min);
	            $this.val(val);
	        });
	    }
	};






    // Product Detail Init
    RESHOP.productDetailInit = function() {
      if ($productDetailElement.length && $productDetailElementThumbnail.length) {

          var ELEVATE_ZOOM_OBJ = {
              borderSize: 1,
              autoWidth:true,
              zoomWindowWidth: 540,
              zoomWindowHeight: 540,
              zoomWindowOffetx: 10,
              borderColour: '#e9e9e9',
              cursor: 'pointer'
          };
            // Fires after first initialization
          $productDetailElement.on('init', function () {
              $(this).closest('.slider-fouc').removeClass('slider-fouc');
          });

          $productDetailElement.slick({
              slidesToShow: 1,
              slidesToScroll: 1,
              infinite:false,
              arrows: false,
              dots: false,
              fade: true,
              asNavFor: $productDetailElementThumbnail
          });
          // Init elevate zoom plugin to the first image
          $('#pd-o-initiate .slick-current img').elevateZoom(ELEVATE_ZOOM_OBJ);

          // Fires before slide change
          $productDetailElement.on('beforeChange', function(event, slick, currentSlide, nextSlide){
              // Get the next slide image
              var $img = $(slick.$slides[nextSlide]).find('img');
              // Remove old zoom elements
              $('.zoomWindowContainer,.zoomContainer').remove();
              // Reinit elevate zoom plugin to the next slide image
              $($img).elevateZoom(ELEVATE_ZOOM_OBJ);
          });

          // Init Lightgallery plugin
          $productDetailElement.lightGallery({
              selector: '.pd-o-img-wrap',// lightgallery-core
              download: false,// lightgallery-core
              thumbnail: false,// Thumbnails
              autoplayControls: false,// Autoplay-plugin
              actualSize: false,// Zoom-plugin: Enable actual pixel icon
              hash: false, // Hash-plugin
              share: false// share-plugin
          });
          // Thumbnail images
          // Fires after first initialization
          $productDetailElementThumbnail.on('init', function () {
              $(this).closest('.slider-fouc').removeAttr('class');
          });

          $productDetailElementThumbnail.slick({
              slidesToShow: 4,
              slidesToScroll: 1,
              infinite:false,
              arrows: true,
              dots: false,
              focusOnSelect: true,
              asNavFor: $productDetailElement,
              prevArrow:'<div class="pt-prev"><i class="fas fa-angle-left"></i>',
              nextArrow:'<div class="pt-next"><i class="fas fa-angle-right"></i>',
              responsive: [
                  {
                      breakpoint: 1200,
                      settings: {
                          slidesToShow: 4
                      }
                  },
                  {
                      breakpoint: 992,
                      settings: {
                          slidesToShow: 3
                      }
                  },
                  {
                      breakpoint: 576,
                      settings: {
                          slidesToShow: 2
                      }
                  }
              ]
          });
      }
    };

	// Modal Product Detail Init
	RESHOP.modalProductDetailInit = function() {
	    // Ép quét lại DOM mỗi khi gọi hàm
	    var $dynamicModalDetail = $('#js-product-detail-modal');
	    var $dynamicModalThumbnail = $('#js-product-detail-modal-thumbnail');

	    if ($dynamicModalDetail.length && $dynamicModalThumbnail.length) {
	        // Hủy Slick cũ nếu đã từng khởi tạo trước đó (để không bị lỗi dồn file)
	        if ($dynamicModalDetail.hasClass('slick-initialized')) {
	            $dynamicModalDetail.slick('unslick');
	        }
	        if ($dynamicModalThumbnail.hasClass('slick-initialized')) {
	            $dynamicModalThumbnail.slick('unslick');
	        }

	        $dynamicModalDetail.slick({
	            slidesToShow: 1,
	            slidesToScroll: 1,
	            infinite:false,
	            arrows: false,
	            dots: false,
	            fade: true,
	            asNavFor: $dynamicModalThumbnail
	        });

	        $dynamicModalThumbnail.slick({
	            slidesToShow: 4,
	            slidesToScroll: 1,
	            infinite:false,
	            arrows: true,
	            dots: false,
	            focusOnSelect: true,
	            asNavFor: $dynamicModalDetail,
	            prevArrow:'<div class="pt-prev"><i class="fas fa-angle-left"></i>',
	            nextArrow:'<div class="pt-next"><i class="fas fa-angle-right"></i>',
	            responsive: [
	                { breakpoint: 1200, settings: { slidesToShow: 4 } },
	                { breakpoint: 992, settings: { slidesToShow: 3 } },
	                { breakpoint: 576, settings: { slidesToShow: 2 } }
	            ]
	        });

	        // Cập nhật lại kích thước khi Modal mở ra
	        $('#quick-look').on('shown.bs.modal', function () {
	            $dynamicModalDetail.resize();
	        });
	    }
	};
    // Shop Category Toggle Functionality
    RESHOP.shopCategoryToggle = function() {
        if ($shopCategoryToggleSpan.length) {
            $shopCategoryToggleSpan.on('click', function () {
                $(this).toggleClass('is-expanded');
                $(this).next('ul').stop(true, true).slideToggle();
            });
        }
    };



    // Shop Perspective Change
    RESHOP.shopPerspectiveChange = function() {
          if ($shopGridBtn.length && $shopListBtn.length)   {
              $shopGridBtn.on('click',function () {
                  $(this).addClass('is-active');
                  $shopListBtn.removeClass('is-active');
                  $shopPerspectiveRow.removeClass('is-list-active');
                  $shopPerspectiveRow.addClass('is-grid-active');
              });
              $shopListBtn.on('click',function () {
                  $(this).addClass('is-active');
                  $shopGridBtn.removeClass('is-active');
                  $shopPerspectiveRow.removeClass('is-grid-active');
                  $shopPerspectiveRow.addClass('is-list-active');
              });
          }
    };
    // Shop Side Filter Settings
    RESHOP.shopSideFilter = function() {
        if ($shopFilterBtn.length) {
            $shopFilterBtn.on('click',function () {
                // Add Class Active
                $(this).toggleClass('is-active');
                // Get Value of the attribute data-side
                var target = $(this).attr('data-side');
                // Open Side
                $(target).toggleClass('is-open');
            });
        }
    };



    // Check everything including DOM elements and images loaded
    $(window).on('load',function () {
        if ($primarySlider.length) {
            // Play slider when everything is loaded
            $primarySlider.data('owl.carousel').options.autoplay = true;
            $primarySlider.trigger('refresh.owl.carousel');
        }
    });

    // Countdown Component Initialization
    RESHOP.initCountdown = function() {
        var $countdownElements = $('[data-countdown]');
        if ($countdownElements.length) {
            $countdownElements.each(function() {
                var $this = $(this), finalDate = $(this).data('countdown');
                $this.countdown(finalDate, function(event) {
                    $this.html(event.strftime('<div class="countdown__content"><span class="countdown__value">%D</span><span class="countdown__key">Ngày</span></div><div class="countdown__content"><span class="countdown__value">%H</span><span class="countdown__key">Giờ</span></div><div class="countdown__content"><span class="countdown__value">%M</span><span class="countdown__key">Phút</span></div><div class="countdown__content"><span class="countdown__value">%S</span><span class="countdown__key">Giây</span></div>'));
                });
            });
        }
    };

    // Filter Component with smooth CSS animation (replaces Isotope)
    RESHOP.initIsotopeFilter = function() {
        var $wrapper = $('.filter__grid-wrapper');
        var $btns = $('.filter__btn');
        
        if ($wrapper.length) {
            var $row = $wrapper.find('.row');
            var $items = $row.find('.filter__item');
            
            // Show top 14 items initially for 'TẤT CẢ' tab (row 4 product 2)
            $items.css({'transition': 'all 0.35s ease'});
            $items.each(function(idx) {
                if (idx < 14) {
                    $(this).css({'opacity': '1', 'transform': 'scale(1)'}).show();
                } else {
                    $(this).css({'opacity': '0', 'transform': 'scale(0.8)'}).hide();
                }
            });
            
            $btns.on('click', function() {
                var filterValue = $(this).attr('data-filter');
                
                // Update active button style
                $btns.removeClass('js-checked');
                $(this).addClass('js-checked');
                
                if (filterValue === '*') {
                    // Show first 14 items for 'TẤT CẢ' tab, hide the rest
                    $items.each(function(idx) {
                        var $item = $(this);
                        if (idx < 14) {
                            if ($item.css('display') === 'none') {
                                $item.css({'opacity': '0', 'transform': 'scale(0.8)'}).show();
                                setTimeout(function() {
                                    $item.css({'opacity': '1', 'transform': 'scale(1)'});
                                }, 50);
                            }
                        } else {
                            $item.css({'opacity': '0', 'transform': 'scale(0.8)'});
                            setTimeout(function() {
                                $item.hide();
                            }, 350);
                        }
                    });
                } else {
                    // Filter by brand name using data-brand attribute with normalized brand matching
                    function normalizeBrandName(str) {
                        if (!str) return '';
                        var s = str.toLowerCase().trim().replace(/[\s\-_]/g, '');
                        if (s.indexOf('lining') !== -1) return 'lining';
                        return s;
                    }
                    var normFilter = normalizeBrandName(filterValue);
                    var matchCount = 0;

                    $items.each(function() {
                        var $item = $(this);
                        var itemBrand = $item.attr('data-brand') || '';
                        var normItem = normalizeBrandName(itemBrand);
                        var isMatch = (normItem === normFilter) || (normFilter === 'lining' && normItem === 'lining');

                        if (isMatch && matchCount < 14) {
                            matchCount++;
                            // Show matching items (up to max 14)
                            if ($item.css('display') === 'none') {
                                $item.css({'opacity': '0', 'transform': 'scale(0.8)'}).show();
                                setTimeout(function() {
                                    $item.css({'opacity': '1', 'transform': 'scale(1)'});
                                }, 50);
                            }
                        } else {
                            // Hide non-matching or excess items with animation
                            $item.css({'opacity': '0', 'transform': 'scale(0.8)'});
                            setTimeout(function() {
                                $item.hide();
                            }, 350);
                        }
                    });
                }
            });
        }
    };


    try { RESHOP.initScrollUp(); } catch (e) { console.error("Error in initScrollUp:", e); }
    try { RESHOP.initTooltip(); } catch (e) { console.error("Error in initTooltip:", e); }
    try { RESHOP.initModal(); } catch (e) { console.error("Error in initModal:", e); }
    try { RESHOP.defaultAddressCheckbox(); } catch (e) { console.error("Error in defaultAddressCheckbox:", e); }
    try { RESHOP.initScrollSpy(); } catch (e) { console.error("Error in initScrollSpy:", e); }
    try { RESHOP.onClickScroll(); } catch (e) { console.error("Error in onClickScroll:", e); }
    try { RESHOP.reshopNavigation(); } catch (e) { console.error("Error in reshopNavigation:", e); }
    try { RESHOP.primarySlider(); } catch (e) { console.error("Error in primarySlider:", e); }
    try { RESHOP.productSlider(); } catch (e) { console.error("Error in productSlider:", e); }
    try { RESHOP.tabSlider(); } catch (e) { console.error("Error in tabSlider:", e); }
    try { RESHOP.onTabActiveRefreshSlider(); } catch (e) { console.error("Error in onTabActiveRefreshSlider:", e); }
    try { RESHOP.brandSlider(); } catch (e) { console.error("Error in brandSlider:", e); }
    try { RESHOP.testimonialSlider(); } catch (e) { console.error("Error in testimonialSlider:", e); }
    try { RESHOP.appConfiguration(); } catch (e) { console.error("Error in appConfiguration:", e); }
    try { RESHOP.initInputCounter(); } catch (e) { console.error("Error in initInputCounter:", e); }
    try { RESHOP.productDetailInit(); } catch (e) { console.error("Error in productDetailInit:", e); }
    try { RESHOP.modalProductDetailInit(); } catch (e) { console.error("Error in modalProductDetailInit:", e); }
    try { RESHOP.initCountdown(); } catch (e) { console.error("Error in initCountdown:", e); }
    try { RESHOP.initIsotopeFilter(); } catch (e) { console.error("Error in initIsotopeFilter:", e); }
    try { RESHOP.shopCategoryToggle(); } catch (e) { console.error("Error in shopCategoryToggle:", e); }
    try { RESHOP.shopPerspectiveChange(); } catch (e) { console.error("Error in shopPerspectiveChange:", e); }
    try { RESHOP.shopSideFilter(); } catch (e) { console.error("Error in shopSideFilter:", e); }
    window.RESHOP = RESHOP;
})(jQuery);

/*==============================================================
  # CUSTOM JS: Xử lý Logic Chọn Phân Loại (Không dùng ID)
  ==============================================================*/
function selectColor(element) {
    if (!element) return;
    let container = element.closest('.pd-detail'); 
    if (!container) return;
    
    container.querySelectorAll('.color-btn').forEach(btn => btn.classList.remove('active'));
    element.classList.add('active');
    
    container.setAttribute('data-selected-color', element.getAttribute('data-color'));
    checkAndApplyVariant(container);
}

function selectSize(element) {
    if (!element) return;
    let container = element.closest('.pd-detail');
    if (!container) return;
    
    container.querySelectorAll('.size-btn').forEach(btn => btn.classList.remove('active'));
    element.classList.add('active');
    
    container.setAttribute('data-selected-size', element.getAttribute('data-size'));
    checkAndApplyVariant(container);
}

function resolveImageUrl(imgPath) {
    if (!imgPath) return '/images/placeholder.png';
    var s = imgPath.trim();
    if (s.startsWith('/')) return s;
    if (s.startsWith('uploads/')) return '/' + s;
    return '/uploads/product/' + s;
}

function selectDynamicAttribute(element) {
    if (!element) return;
    let container = element.closest('.pd-detail');
    if (!container) return;

    const attrName = element.getAttribute('data-attr-name');
    const attrVal = element.getAttribute('data-attr-value');

    // Remove active class from buttons in the same attribute group
    container.querySelectorAll('.dynamic-attr-btn').forEach(btn => {
        if (btn.getAttribute('data-attr-name') === attrName) {
            btn.classList.remove('active');
        }
    });
    element.classList.add('active');

    // Store in container
    const sanitizedKey = 'data-selected-attr-' + attrName.replace(/\s+/g, '_').toLowerCase();
    container.setAttribute(sanitizedKey, attrVal);
    checkAndApplyVariant(container);
}

function checkAndApplyVariant(container) {
    if (!container) return;
    const btnAdd = container.querySelector('.js-btn-add-cart');
    const stockStatus = container.querySelector('.js-stock-status');
    const inputId = container.querySelector('.js-variant-id');
    const quantityInput = container.querySelector('.js-quantity-input');
    
    const priceDisplay = container.querySelector('.js-display-price') || container.querySelector('.pd-detail__price') || document.getElementById('js-display-price');
    const originalPriceDisplay = container.querySelector('.js-display-original-price') || container.querySelector('.pd-detail__del');
    const discountBadgeDisplay = container.querySelector('.js-display-discount-badge') || container.querySelector('.pd-detail__discount');
    
    const stockInfoPanel = container.querySelector('.js-variant-stock-info') || document.getElementById('js-variant-stock-info');
    const stockCountEl = container.querySelector('.js-variant-stock-count') || document.getElementById('js-variant-stock-count');
    const stockBadgeEl = container.querySelector('.js-variant-stock-badge') || document.getElementById('js-variant-stock-badge');

    const selectedColor = container.getAttribute('data-selected-color');
    const selectedSize = container.getAttribute('data-selected-size');

    const hasColorOptions = container.querySelector('.color-btn') !== null;
    const hasSizeOptions = container.querySelector('.size-btn') !== null;

    // Scan dynamic attributes
    const dynamicButtons = container.querySelectorAll('.dynamic-attr-btn');
    const dynamicAttrNames = Array.from(new Set(Array.from(dynamicButtons).map(btn => btn.getAttribute('data-attr-name'))));
    
    let hasMissingAttr = false;
    const selectedAttributes = {};
    for (const name of dynamicAttrNames) {
        const key = 'data-selected-attr-' + name.replace(/\s+/g, '_').toLowerCase();
        const val = container.getAttribute(key);
        if (!val) {
            hasMissingAttr = true;
        } else {
            selectedAttributes[name] = val;
        }
    }

    const btnBuyNow = container.querySelector('.js-btn-buy-now');

    if ((hasColorOptions && !selectedColor) || (hasSizeOptions && !selectedSize) || hasMissingAttr) {
        if (inputId) inputId.value = '';
        if(stockStatus) {
            stockStatus.style.display = 'block';
            stockStatus.innerHTML = '<i class="fas fa-info-circle"></i> Vui lòng chọn đầy đủ các thuộc tính phân loại.';
            stockStatus.className = 'js-stock-status u-s-m-b-15 text-warning fw-bold';
        }
        if (stockInfoPanel) stockInfoPanel.style.display = 'none';
        if (btnAdd) {
            btnAdd.disabled = false;
            btnAdd.innerText = 'Thêm vào giỏ';
            btnAdd.style.backgroundColor = '';
            btnAdd.style.borderColor = '';
        }
        if (btnBuyNow) {
            btnBuyNow.disabled = false;
            btnBuyNow.innerText = 'Mua ngay';
            btnBuyNow.style.backgroundColor = '#e65100';
            btnBuyNow.style.borderColor = '#e65100';
        }
        return;
    }

    const variants = container.danhSachBienTo || container.danhSachBienThe;
    if (typeof variants === 'undefined' || !variants) {
        console.error("Lỗi: Không tìm thấy danh sách biến thể trên container!", container);
        if (stockStatus) {
            stockStatus.style.display = 'block';
            stockStatus.innerHTML = '<i class="fas fa-exclamation-triangle"></i> Lỗi hệ thống: Không tải được thông tin phân loại.';
            stockStatus.className = 'js-stock-status u-s-m-b-15 text-danger fw-bold';
        }
        return;
    }

    const matchedVariant = variants.find(v => {
        const matchColor = !hasColorOptions || (v.mauSac && selectedColor && v.mauSac.trim().toLowerCase() === selectedColor.trim().toLowerCase());
        const matchSize = !hasSizeOptions || (selectedSize && ((v.trongLuong && v.trongLuong.trim().toLowerCase() === selectedSize.trim().toLowerCase()) || (v.kichThuoc && v.kichThuoc.trim().toLowerCase() === selectedSize.trim().toLowerCase())));
        
        let matchDynamic = true;
        if (v.attributes) {
            for (const name in selectedAttributes) {
                const vVal = v.attributes[name];
                const selVal = selectedAttributes[name];
                if (!vVal || vVal.trim().toLowerCase() !== selVal.trim().toLowerCase()) {
                    matchDynamic = false;
                    break;
                }
            }
        } else {
            for (const name in selectedAttributes) {
                const selVal = selectedAttributes[name].trim().toLowerCase();
                if (name === "Màu sắc") {
                    if (!v.mauSac || v.mauSac.trim().toLowerCase() !== selVal) matchDynamic = false;
                } else if (name === "Kích thước") {
                    if (!v.kichThuoc || v.kichThuoc.trim().toLowerCase() !== selVal) matchDynamic = false;
                } else if (name === "Trọng lượng") {
                    if (!v.trongLuong || v.trongLuong.trim().toLowerCase() !== selVal) matchDynamic = false;
                } else if (name === "Mức căng" || name === "Sức căng") {
                    const vCang = v.mucCang || v.sucCang;
                    if (!vCang || vCang.trim().toLowerCase() !== selVal) matchDynamic = false;
                }
            }
        }
        return matchColor && matchSize && matchDynamic;
    });

    if (matchedVariant) {
        // --- CẬP NHẬT THÔNG TIN CƠ BẢN ---
        if (inputId) inputId.value = matchedVariant.id;
        
        const soLuong = matchedVariant.soLuongTon || 0;
        const conHang = soLuong > 0;
        
        if (btnAdd) {
            btnAdd.disabled = !conHang;
            if (conHang) {
                btnAdd.innerText = 'Thêm vào giỏ';
                btnAdd.style.backgroundColor = '';
                btnAdd.style.borderColor = '';
            } else {
                btnAdd.innerText = 'Hết hàng';
                btnAdd.style.backgroundColor = '#a0a0a0';
                btnAdd.style.borderColor = '#a0a0a0';
            }
        }
        if (btnBuyNow) {
            btnBuyNow.disabled = !conHang;
            if (conHang) {
                btnBuyNow.innerText = 'Mua ngay';
                btnBuyNow.style.backgroundColor = '#e65100';
                btnBuyNow.style.borderColor = '#e65100';
            } else {
                btnBuyNow.innerText = 'Hết hàng';
                btnBuyNow.style.backgroundColor = '#a0a0a0';
                btnBuyNow.style.borderColor = '#a0a0a0';
            }
        }
        
        // Ẩn thông báo cảnh báo
        if(stockStatus) {
            stockStatus.style.display = 'none';
        }

        // Hiển thị panel tồn kho riêng
        if (stockInfoPanel) {
            stockInfoPanel.style.display = 'block';
            if (stockCountEl) {
                stockCountEl.innerText = soLuong + ' sản phẩm';
                stockCountEl.style.color = conHang ? '#009444' : '#ff4500';
            }
            if (stockBadgeEl) {
                if (conHang) {
                    stockBadgeEl.innerText = 'Còn hàng';
                    stockBadgeEl.className = 'js-variant-stock-badge pd-detail__stock';
                    stockBadgeEl.style.display = 'inline-block';
                } else {
                    stockBadgeEl.innerText = 'Hết hàng';
                    stockBadgeEl.className = 'js-variant-stock-badge pd-detail__left';
                    stockBadgeEl.style.display = 'inline-block';
                }
            }
            
            // Xử lý warning badge cho quick look
            const lowStockEl = container.querySelector('.js-variant-low-stock');
            if (lowStockEl) {
                if (conHang && soLuong <= 5) {
                    lowStockEl.innerText = 'Chỉ còn ' + soLuong + ' sản phẩm';
                    lowStockEl.style.display = 'inline-block';
                } else {
                    lowStockEl.style.display = 'none';
                }
            }
        }

        if (quantityInput) quantityInput.setAttribute('data-max', soLuong);
        if (priceDisplay) {
            if (matchedVariant.phanTramGiam > 0) {
                priceDisplay.innerText = new Intl.NumberFormat('vi-VN').format(matchedVariant.giaSauGiam) + " đ";
                if (originalPriceDisplay) {
                    originalPriceDisplay.innerText = new Intl.NumberFormat('vi-VN').format(matchedVariant.giaBan) + " đ";
                    originalPriceDisplay.style.display = 'inline-block';
                }
                if (discountBadgeDisplay) {
                    discountBadgeDisplay.innerText = '(Giảm ' + matchedVariant.phanTramGiam + '%)';
                    discountBadgeDisplay.style.display = 'inline-block';
                }
            } else {
                priceDisplay.innerText = new Intl.NumberFormat('vi-VN').format(matchedVariant.giaBan) + " đ";
                if (originalPriceDisplay) {
                    originalPriceDisplay.style.display = 'none';
                }
                if (discountBadgeDisplay) {
                    discountBadgeDisplay.style.display = 'none';
                }
            }
        }

        // --- CẬP NHẬT HÌNH ẢNH (TÍCH HỢP CẢ QUICK LOOK & DETAIL) ---
        // Tìm ngược lên thẻ bọc ngoài cùng (div class="row") để qua cột trái lấy ảnh
        const mainRow = container.closest('.row');
        
        if (mainRow && matchedVariant.hinhAnhSanPham) {
            const newImgSrc = resolveImageUrl(matchedVariant.hinhAnhSanPham);

            // Ưu tiên tìm ảnh đang active trong slider Slick (Của Modal hoặc Detail)
            let activeImage = mainRow.querySelector('#js-product-detail-modal .slick-current img') ||
                              mainRow.querySelector('#pd-o-initiate .slick-current img');

            // Fallback: Nếu Slick chưa init xong, lấy ảnh đầu tiên nó thấy
            if (!activeImage) {
                activeImage = mainRow.querySelector('#js-product-detail-modal img') ||
                              mainRow.querySelector('#pd-o-initiate img');
            }

            if (activeImage) {
                // Đổi đường dẫn ảnh hiển thị
                activeImage.onerror = function() {
                    this.onerror = null;
                    this.src = '/images/placeholder.png';
                };
                activeImage.src = newImgSrc;

                // Nếu ảnh có plugin Kính lúp (ElevateZoom) ở trang Chi tiết
                if (activeImage.hasAttribute('data-zoom-image')) {
                    activeImage.setAttribute('data-zoom-image', newImgSrc);
                    
                    // Gọi API của plugin ElevateZoom để nó nạp lại ảnh phóng to mới
                    let ez = $(activeImage).data('elevateZoom');
                    if (ez) {
                        ez.swaptheimage(newImgSrc, newImgSrc);
                    }
                }
            }
        }

    } else {
        // Trường hợp hết hàng / Không có biến thể này
        if (inputId) inputId.value = "";
        if (btnAdd) {
            btnAdd.disabled = true;
            btnAdd.innerText = 'Hết hàng';
            btnAdd.style.backgroundColor = '#a0a0a0';
            btnAdd.style.borderColor = '#a0a0a0';
        }
        if (btnBuyNow) {
            btnBuyNow.disabled = true;
            btnBuyNow.innerText = 'Hết hàng';
            btnBuyNow.style.backgroundColor = '#a0a0a0';
            btnBuyNow.style.borderColor = '#a0a0a0';
        }
        if (stockInfoPanel) stockInfoPanel.style.display = 'none';
        if(stockStatus) {
            stockStatus.style.display = 'block';
            stockStatus.innerHTML = '<i class="fas fa-times-circle"></i> Hiện đã hết hàng.';
            stockStatus.className = 'js-stock-status u-s-m-b-15 text-danger fw-bold';
        }
    }
}
/*==============================================================
  # CUSTOM JS: Xử lý Quick Look Modal bằng AJAX
  ==============================================================*/
  function openQuickLookModal(productId) {
      $.ajax({
          url: '/modal/quick-look/' + productId,
          type: 'GET',
          success: function(responseHtml) {
              $('#quick-look-content').html(responseHtml);
              
              // 1. Kích hoạt lại nút cộng trừ số lượng
              if(typeof RESHOP.initInputCounter === 'function') {
                  RESHOP.initInputCounter();
              }

              // 2. Kích hoạt lại Slider ảnh cho đúng form
              if(typeof RESHOP.modalProductDetailInit === 'function') {
                  RESHOP.modalProductDetailInit();
              }
              
              $('#quick-look').modal('show');
          },
          error: function(error) {
              showToast("Có lỗi xảy ra khi tải dữ liệu sản phẩm!", "error");
          }
      });
  }
  /*==============================================================
    # CUSTOM JS: Load dữ liệu Giỏ hàng thu nhỏ (Mini Cart) bằng AJAX
    ==============================================================*/
  function loadMiniCart() {
      $.ajax({
          url: '/gio-hang/api/mini-cart',
          type: 'GET',
          success: function(response) {
              // 1. Cập nhật số lượng đỏ cho CẢ Header VÀ Floating Cart
              $('#cart-icon-count, #floating-cart-count').text(response.tongSoLuong);

              // 2. Gom Selector: Chọn CẢ 2 khu vực chứa danh sách
              var $cartContainer = $('#mini-cart-items, #floating-mini-cart-items');
              var $cartTotal = $('#mini-cart-total, #floating-mini-cart-total');

              // Xử lý trường hợp trống
              if (response.trangThai === 'chuadangnhap' || response.tongSoLuong === 0) {
                  $cartContainer.html('<div class="text-center u-s-p-y-15">Giỏ hàng của bạn đang trống.</div>');
                  $cartTotal.text('0 đ');
                  return; 
              }

              // 3. Format tiền tệ VNĐ và cập nhật Tổng tiền cho CẢ 2
              var formattedTotal = new Intl.NumberFormat('vi-VN').format(response.tongTien) + ' đ';
              $cartTotal.text(formattedTotal);

              // 4. Lặp qua danh sách sản phẩm và tạo thẻ HTML
              var htmlContent = '';
              response.danhSach.forEach(function(item) {
                  var formattedPrice = new Intl.NumberFormat('vi-VN').format(item.giaBan) + ' đ';
                  var productUrl = '/san-pham/' + item.idSanPham;
                  var imageUrl = resolveImageUrl(item.hinhAnh);

                  var variantHtml = '';
                  var hasMau = item.mauSac && item.mauSac.trim().length > 0;
                  var hasTrongLuong = item.trongLuong && item.trongLuong.trim().length > 0;
                  if (hasMau || hasTrongLuong) {
                      variantHtml += '<div class="mini-product__variants">';
                      if (hasMau) {
                          variantHtml += `
                              <span class="mini-product__badge">
                                  <i class="fas fa-palette"></i>
                                  <span>Màu:</span>
                                  <strong>${item.mauSac}</strong>
                              </span>`;
                      }
                      if (hasTrongLuong) {
                          variantHtml += `
                              <span class="mini-product__badge">
                                  <i class="fas fa-ruler-combined"></i>
                                  <span>Size:</span>
                                  <strong>${item.trongLuong}</strong>
                              </span>`;
                      }
                      variantHtml += '</div>';
                  }

                  // TẠO HTML CHO TỪNG SẢN PHẨM TRONG GIỎ (Dùng Template Literal)
                  htmlContent += `
                      <div class="card-mini-product">
                          <div class="mini-product">
                              <div class="mini-product__image-wrapper">
                                  <a class="mini-product__link" href="${productUrl}">
                                      <img class="u-img-fluid" src="${imageUrl}" alt="">
                                  </a>
                              </div>
                              <div class="mini-product__info-wrapper">
                                  <span class="mini-product__name">
                                      <a href="${productUrl}">${item.tenSanPham}</a>
                                  </span>
                                  ${variantHtml}
                                  <span class="mini-product__quantity">${item.soLuong} x</span>
                                  <span class="mini-product__price">${formattedPrice}</span>
                              </div>
                          </div>
						  <a class="mini-product__delete-link far fa-trash-alt js-delete-cart-item" 
						     href="javascript:void(0)" 
						     data-id="${item.idChiTiet}">
						  </a>
                      </div>
                  `;
              });

              // 5. Bơm toàn bộ HTML vừa tạo vào CẢ 2 khung
              $cartContainer.html(htmlContent);
          },
          error: function(error) {
              showToast("Không thể tải giỏ hàng. Vui lòng thử lại.", "error");
          }
      });
  }

  // Lệnh này ép trình duyệt: "Ngay khi trang web vừa load xong thì chạy hàm loadMiniCart() ngay cho tao!"
  $(document).ready(function() {
      loadMiniCart();
      
      // Tự động chạy checkAndApplyVariant cho trang chi tiết sản phẩm khi vừa load trang
      var detailContainer = document.getElementById('product-detail-container');
      if (detailContainer && typeof checkAndApplyVariant === 'function') {
          checkAndApplyVariant(detailContainer);
      }
  });
  /*==============================================================
    # CUSTOM JS: Xử lý Thêm vào giỏ hàng bằng AJAX (Chống reload trang)
    ==============================================================*/
  $(document).on('submit', '.pd-detail__form', function(e) {
      var form = $(this);
      
      // 1. Kiểm tra xem đã chọn thuộc tính chưa
      var container = form.closest('.pd-detail');
      var spctId = form.find('.js-variant-id').val();
      if (container.length) {
          var selectedColor = container.attr('data-selected-color');
          var selectedSize = container.attr('data-selected-size');
          var hasColorOptions = container.find('.color-btn').length > 0;
          var hasSizeOptions = container.find('.size-btn').length > 0;
          
          var dynamicButtons = container.find('.dynamic-attr-btn');
          var dynamicAttrNames = Array.from(new Set(Array.from(dynamicButtons).map(btn => $(btn).attr('data-attr-name'))));
          var hasMissingDynamic = false;
          for (var i = 0; i < dynamicAttrNames.length; i++) {
              var key = 'data-selected-attr-' + dynamicAttrNames[i].replace(/\s+/g, '_').toLowerCase();
              if (!container.attr(key)) {
                  hasMissingDynamic = true;
                  break;
              }
          }
          
          if ((hasColorOptions && !selectedColor) || (hasSizeOptions && !selectedSize) || hasMissingDynamic || !spctId) {
              e.preventDefault();
              var stockStatus = container.find('.js-stock-status');
              if (stockStatus.length) {
                  stockStatus.html('<i class="fas fa-exclamation-circle"></i> Vui lòng chọn đầy đủ các thuộc tính phân loại sản phẩm.');
                  stockStatus.attr('class', 'js-stock-status u-s-m-b-15 text-danger fw-bold');
                  stockStatus.css('display', 'block');
              }
              if (typeof showToast === 'function') {
                  showToast('Vui lòng chọn đầy đủ các thuộc tính phân loại sản phẩm trước khi thêm vào giỏ hàng!', 'warning');
              }
              return false;
          }
      }

      e.preventDefault(); // Ngăn chặn hành vi reload trang mặc định của form
      
      var url = form.attr('action');
      var data = form.serialize(); // Lấy tự động idSanPhamChiTiet và soLuong

      // Reset Quick Add button token state before sending request
      const $quickAddBtn = $('.js-quick-add-checkout-btn, #js-quick-add-checkout-btn');
      $quickAddBtn.removeData('checkout-url').removeAttr('data-checkout-url').prop('disabled', true);

      // Khóa nút bấm lại để tránh khách click đúp 2 lần
      var submitBtn = form.find('button[type="submit"]');
      var originalBtnText = submitBtn.html();
      submitBtn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i> Đang xử lý...');

      $.ajax({
          url: url,
          type: 'POST',
          data: data,
          success: function(response) {
              // Nhả nút bấm ra
              submitBtn.prop('disabled', false).html(originalBtnText);

              if (response.trangThai === 'chuadangnhap') {
                  window.location.href = '/user/dang-nhap?loi=' + encodeURIComponent('Bạn chưa đăng nhập. Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng!');
                  return;
              }

              if (response.trangThai === 'ok') {
                  // 1. Đổ dữ liệu thật vào Modal
                  $('#js-modal-cart-name').text(response.tenSanPham);
                  $('#js-modal-cart-variant').text('Phân loại: ' + response.phanLoai);
                  $('#js-modal-cart-qty').text('Số lượng thêm: ' + response.soLuongThem);
                  
                  var formattedPrice = new Intl.NumberFormat('vi-VN').format(response.giaBan) + ' đ';
                  $('#js-modal-cart-price').text(formattedPrice);
                  
                  $('#js-modal-cart-img').attr('src', resolveImageUrl(response.hinhAnh));

                  // 2. Ẩn modal Quick Look nếu khách đang thao tác từ Quick Look
                  $('#quick-look').modal('hide');

                  if (response.quickAddCheckoutUrl) {
                      $quickAddBtn.attr('data-checkout-url', response.quickAddCheckoutUrl)
                                  .data('checkout-url', response.quickAddCheckoutUrl)
                                  .prop('disabled', false);
                  }
                  // 3. Hiển thị modal thông báo thành công
                  $('#add-to-cart').modal('show');


                  // 4. Update tự động Mini Cart trên thanh Header
                  if (typeof loadMiniCart === 'function') {
                      loadMiniCart();
                  }
              } else {
                  showToast("Lỗi: " + response.message, "error");
              }
          },
          error: function(xhr) {
              submitBtn.prop('disabled', false).html(originalBtnText);
              var rawMsg = (xhr && xhr.responseText) ? xhr.responseText : "Có lỗi kết nối đến máy chủ. Vui lòng thử lại!";
              var message = getFriendlyErrorMessage(rawMsg, "Không thể kết nối đến máy chủ. Vui lòng thử lại sau.");
              showToast(message, "error");
          }
      });
  });
  
  /*==============================================================
    # CUSTOM JS: Xóa sản phẩm bằng AJAX (Hiệu ứng mượt, không nhảy trang)
    ==============================================================*/
  $(document).on('click', '.js-delete-cart-item', function(e) {
      e.preventDefault(); // Khóa cứng hành vi load/nhảy trang mặc định
      
      var btn = $(this);
      var idChiTiet = btn.data('id');

      SmashNotify.confirm({
          title: 'Xóa sản phẩm khỏi giỏ?',
          message: 'Bạn có chắc chắn muốn xóa sản phẩm này khỏi giỏ?',
          confirmText: 'Xóa',
          danger: true
      }).then(function(confirmed) {
          if (!confirmed) return;
          // Làm mờ nút đi một chút để báo hiệu đang chờ Server xử lý
          btn.css('opacity', '0.5');

          $.ajax({
              url: '/gio-hang/api/xoa/' + idChiTiet,
              type: 'POST',
              success: function(response) {
                  if (response.trangThai === 'chuadangnhap') {
                      window.location.href = '/user/dang-nhap';
                      return;
                  }
                  
                  if (response.trangThai === 'ok') {
                      // 1. NẾU ĐANG Ở TRANG CART.HTML: Xóa dòng <tr> với hiệu ứng mờ dần
                      var row = btn.closest('tr');
                      if (row.length) {
                          row.fadeOut(300, function() {
                              $(this).remove(); // Cắt bỏ thẻ <tr> khỏi giao diện

                              if (typeof recalculateSelectedCartSummary === 'function') {
                                  recalculateSelectedCartSummary();
                              }
                              
                              // Nếu bảng trống trơn, hiển thị thông báo "Giỏ hàng trống"
                              if ($('table.table-p tbody tr.js-cart-row').length === 0) {
                                  $('table.table-p tbody').html('<tr><td colspan="5" class="text-center u-s-p-y-30">Giỏ hàng của bạn đang trống. Hãy thêm sản phẩm vào giỏ nhé!</td></tr>');
                                  $('.f-cart').fadeOut(); // Ẩn luôn khu vực Tạm tính tiền
                              }
                          });
                      }


                      // 2. Tái sử dụng lại API loadMiniCart để nó tự lo việc tính toán TỔNG TIỀN và SỐ LƯỢNG MỚI
                      if (typeof loadMiniCart === 'function') {
                          loadMiniCart();
                      }

                  } else {
                      showToast("Có lỗi xảy ra khi xóa!", "error");
                      btn.css('opacity', '1');
                  }
              },
              error: function() {
                  showToast("Không thể kết nối đến máy chủ. Vui lòng thử lại sau.", "error");
                  btn.css('opacity', '1');
              }
          });
      });
  });
  /*==============================================================
    # CUSTOM JS: Thêm nhanh vào giỏ (Dành cho SP có duy nhất 1 biến thể)
    ==============================================================*/
  function quickAddToCart(idSanPhamChiTiet) {
      const $quickAddBtn = $('.js-quick-add-checkout-btn, #js-quick-add-checkout-btn');
      $quickAddBtn.removeData('checkout-url').removeAttr('data-checkout-url').prop('disabled', true);

      $.ajax({
          url: '/gio-hang/them',
          type: 'POST',
          data: {
              idSanPhamChiTiet: idSanPhamChiTiet,
              soLuong: 1 // Thêm nhanh mặc định là 1 chiếc
          },
          success: function(response) {
              if (response.trangThai === 'chuadangnhap') {
                  window.location.href = '/user/dang-nhap?loi=' + encodeURIComponent('Bạn chưa đăng nhập. Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng!');
                  return;
              }

              if (response.trangThai === 'ok') {
                  // Đổ dữ liệu thật vào Modal Thành Công
                  $('#js-modal-cart-name').text(response.tenSanPham);
                  $('#js-modal-cart-variant').text('Phân loại: ' + response.phanLoai);
                  $('#js-modal-cart-qty').text('Số lượng thêm: ' + response.soLuongThem);
                  
                  var formattedPrice = new Intl.NumberFormat('vi-VN').format(response.giaBan) + ' đ';
                  $('#js-modal-cart-price').text(formattedPrice);
                  $('#js-modal-cart-img').attr('src', resolveImageUrl(response.hinhAnh));

                  if (response.quickAddCheckoutUrl) {
                      $quickAddBtn.attr('data-checkout-url', response.quickAddCheckoutUrl)
                                  .data('checkout-url', response.quickAddCheckoutUrl)
                                  .prop('disabled', false);
                  }
                  // Hiển thị modal thông báo thành công
                  $('#add-to-cart').modal('show');


                  // Load lại Mini Cart trên Header
                  if (typeof loadMiniCart === 'function') {
                      loadMiniCart();
                  }
              } else {
                  showToast("Không thể thêm: " + response.message, "error");
              }
          },
          error: function(xhr) {
              var rawMsg = (xhr && xhr.responseText) ? xhr.responseText : "Có lỗi kết nối đến máy chủ. Vui lòng thử lại!";
              var message = getFriendlyErrorMessage(rawMsg, "Không thể kết nối đến máy chủ. Vui lòng thử lại sau.");
              showToast(message, "error");
          }
      });
  }
  /*==============================================================
    # CUSTOM JS: Xử lý hiển thị Floating Cart khi cuộn chuột
    ==============================================================*/
  $(window).on('scroll', function() {
      // Nếu cuộn chuột xuống quá 300px (nghĩa là đã vượt qua thanh Header)
      if ($(window).scrollTop() > 300) {
          $('#floating-cart').addClass('is-visible');
      } else {
          // Cuộn lên lại đầu trang thì ẩn đi
          $('#floating-cart').removeClass('is-visible');
      }
  });
  /*==============================================================
    # CUSTOM JS: Thêm vào Wishlist bằng AJAX
  ==============================================================*/
  function addToWishlist(idSanPham, element) {
      $.ajax({
          url: '/wishlist/them',
          type: 'POST',
          data: { idSanPham: idSanPham },
          success: function(res) {
              var status = res.status;
              var count = res.count;
              if (status === 'chuadangnhap') {
                  showToast('Vui lòng đăng nhập để sử dụng tính năng yêu thích!', 'info');
                  setTimeout(function() {
                      window.location.href = '/user/dang-nhap';
                  }, 1000);
              } else if (status === 'loi') {
                  showToast(res.message || 'Có lỗi xảy ra, vui lòng thử lại!', 'error');
              } else if (status === 'xoa') {
                  showToast('Đã xóa sản phẩm khỏi danh sách yêu thích!', 'info');
                  if (element) {
                      var $icon = $(element).find('i');
                      if ($icon.hasClass('text-danger') || $icon.closest('.pd-detail__wishlist-btn').length) {
                          $icon.removeClass('fas text-danger').addClass('far');
                      } else {
                          $icon.removeClass('fas').addClass('far');
                          $icon.css('color', '#888');
                      }
                      
                      // Cập nhật số lượng
                      var $countSpan = $(element).find('.js-wishlist-count');
                      if ($countSpan.length) {
                          $countSpan.text('(' + count + ')');
                      }
                      
                      // Cập nhật title của button
                      element.setAttribute('title', 'Yêu thích (' + count + ')');
                  }
              } else if (status === 'ok') {
                  showToast('Đã thêm sản phẩm vào danh sách yêu thích thành công!', 'success');
                  if (element) {
                      var $icon = $(element).find('i');
                      if ($icon.hasClass('text-danger') || $icon.closest('.pd-detail__wishlist-btn').length) {
                          $icon.removeClass('far').addClass('fas text-danger');
                      } else {
                          $icon.removeClass('far').addClass('fas');
                          $icon.css('color', '#ff4500');
                      }
                      
                      // Cập nhật số lượng
                      var $countSpan = $(element).find('.js-wishlist-count');
                      if ($countSpan.length) {
                          $countSpan.text('(' + count + ')');
                      }
                      
                      // Cập nhật title của button
                      element.setAttribute('title', 'Yêu thích (' + count + ')');
                  }
              }
          },
          error: function(xhr) {
              if (xhr.status === 401 || xhr.status === 403) {
                  showToast('Vui lòng đăng nhập để sử dụng tính năng yêu thích!', 'info');
                  setTimeout(function() {
                      window.location.href = '/user/dang-nhap';
                  }, 1000);
              } else {
                  showToast('Có lỗi xảy ra, vui lòng thử lại!', 'error');
              }
          }
      });
  }
  /*==============================================================
    # CUSTOM JS: Xóa sản phẩm Yêu Thích bằng AJAX (Hiệu ứng mượt)
  ==============================================================*/
  $(document).on('click', '.js-delete-wishlist-item', function(e) {
      e.preventDefault();
      var btn = $(this);
      var idSanPham = btn.data('id');

      SmashNotify.confirm({
          title: 'Xóa khỏi danh sách yêu thích?',
          message: 'Bạn có chắc chắn muốn xóa sản phẩm này khỏi danh sách yêu thích?',
          confirmText: 'Xóa',
          danger: true
      }).then(function(confirmed) {
          if (!confirmed) return;
          btn.css('opacity', '0.5');

          $.ajax({
              url: '/wishlist/api/xoa/' + idSanPham,
              type: 'GET',
              success: function(response) {
                  if (response.trangThai === 'chuadangnhap') {
                      window.location.href = '/user/dang-nhap';
                      return;
                  }
                  
                  if (response.trangThai === 'ok') {
                      showToast('Đã xóa sản phẩm khỏi danh sách yêu thích!', 'info');
                      // Tìm thẻ bọc ngoài cùng của sản phẩm (class .w-r) và làm mờ nó đi
                      var row = btn.closest('.w-r');
                      if (row.length) {
                          row.fadeOut(300, function() {
                              $(this).remove(); // Xóa hẳn khỏi HTML
                              
                              // Nếu xóa hết sạch sản phẩm rồi thì hiện thông báo "Danh sách trống"
                              if ($('.w-r').length === 0) {
                                  var emptyHtml = '<div class="text-center u-s-p-y-60"><h3 class="u-s-m-b-15">Danh sách yêu thích của bạn đang trống!</h3><a class="btn btn--e-brand-b-2" href="/">QUAY LẠI MUA SẮM</a></div>';
                                  $('.section__content .col-lg-12.col-md-12.col-sm-12').html(emptyHtml);
                                  $('.route-box').hide(); // Ẩn luôn các nút Xóa Tất Cả bên dưới
                              }
                          });
                      }
                  } else {
                      showToast('Có lỗi xảy ra khi xóa!', 'error');
                      btn.css('opacity', '1');
                  }
              },
              error: function() {
                  showToast('Lỗi kết nối máy chủ!', 'error');
                  btn.css('opacity', '1');
              }
          });
      });
  });
  /*==============================================================
    # CUSTOM JS: Lấy vị trí hiện tại (Geolocation + OpenStreetMap + Bản đồ tương tác)
  ==============================================================*/

  // Global variables for Map and Marker (use var to allow re-declaration if script is re-evaluated by jQuery globalEval)
  var leafletMap = null;
  var mapMarker = null;

  // Debounce helper (Requirement: Debounce reverse geocoding when dragging marker)
  function debounce(func, delay) {
      let timer;
      return function(...args) {
          clearTimeout(timer);
          timer = setTimeout(() => func.apply(this, args), delay);
      };
  }

  // Helper: Fetch with timeout using AbortController (Requirement 4 & 7)
  async function fetchWithTimeout(resource, options = {}) {
      const { timeout = 8000 } = options;
      
      const controller = new AbortController();
      const id = setTimeout(() => controller.abort(), timeout);
      
      try {
          const response = await fetch(resource, {
              ...options,
              signal: controller.signal
          });
          clearTimeout(id);
          return response;
      } catch (error) {
          clearTimeout(id);
          if (error.name === 'AbortError') {
              throw new Error('TIMEOUT_ERROR');
          }
          throw error;
      }
  }

  // Validator: Validate coordinates (Requirement 5)
  function isValidCoordinates(lat, lon) {
      return !isNaN(lat) && !isNaN(lon) && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
  }

  // Reverse geocoding and administrative matching are handled by the backend.
  async function reverseGeocode(latitude, longitude) {
      if (!navigator.onLine) {
          throw new Error("OFFLINE_ERROR");
      }
      if (!window.SmashAddressBook || typeof window.SmashAddressBook.resolveCoordinates !== 'function') {
          throw new Error("GEOCODE_FAILED");
      }
      return window.SmashAddressBook.resolveCoordinates(latitude, longitude);
  }

  // Debounced reverse geocoding to update UI and inputs (Requirement: Debounce reverse geocoding when dragging marker)
  var debouncedReverseGeocode = debounce(async function(lat, lon) {
      try {
          const parsedAddress = await reverseGeocode(lat, lon);
          populateAddressFields(parsedAddress);
          showAddressResolutionResult(parsedAddress, 'Đã cập nhật');
      } catch (err) {
          console.error('[Geocoding] Debounced geocoding error:', err);
          handleGeocodingError(err);
      }
  }, 400);

  // Populate UI inputs with geocoding results
  function populateAddressFields(addressObj) {
      const streetInput = document.getElementById('address-street');

      const addressDetail = addressObj.addressDetail || addressObj.diaChiCuThe;
      if (addressDetail && streetInput) {
          streetInput.value = addressDetail;
          streetInput.dispatchEvent(new Event('input', { bubbles: true }));
      }
  }

  function addressResolutionLevel(addressObj) {
      if (addressObj?.resolutionLevel) return addressObj.resolutionLevel;
      if (addressObj?.wardCode) return 'WARD';
      if (addressObj?.districtId) return 'DISTRICT';
      if (addressObj?.provinceId) return 'PROVINCE';
      return 'NONE';
  }

  function showAddressResolutionResult(addressObj, successTitle) {
      const level = addressResolutionLevel(addressObj);
      const completed = level === 'WARD';
      const partial = level === 'PROVINCE' || level === 'DISTRICT';
      showToast(completed ? successTitle : (partial ? 'Đã điền một phần' : 'Cần kiểm tra lại'),
              addressObj?.message || (completed
                      ? 'Đã tự động cập nhật khu vực theo vị trí.'
                      : 'Không thể xác định chính xác khu vực. Vui lòng chọn địa chỉ bên dưới.'),
              completed ? 'success' : (partial ? 'info' : 'warning'));
  }

  // Update map marker and inputs when getting location from GPS/IP button (Requirement 10)
  function updateMapFromLocation(lat, lon) {
      if (leafletMap && mapMarker) {
          leafletMap.flyTo([lat, lon], 16);
          mapMarker.setLatLng([lat, lon]);
          if (!leafletMap.hasLayer(mapMarker)) {
              mapMarker.addTo(leafletMap);
          }
          
          const latInput = document.getElementById('address-lat');
          const lonInput = document.getElementById('address-lon');
          if (latInput) latInput.value = lat.toFixed(6);
          if (lonInput) lonInput.value = lon.toFixed(6);
      }
  }

  // Centralized geocoding error handler (Requirement 9)
  function handleGeocodingError(error) {
      if (!navigator.onLine || error.message === "OFFLINE_ERROR") {
          showToast('Lỗi kết nối', 'Không có kết nối mạng. Vui lòng thử lại.', 'error');
      } else if (error.message === "RATE_LIMITED") {
          showToast('Giới hạn yêu cầu', 'Bạn đã yêu cầu quá nhanh. Vui lòng đợi một phút trước khi thử lại.', 'warning');
      } else if (error.message === "GEOCODE_FAILED" || error.message === "PROVIDER_ERROR") {
          showToast('Lỗi định vị', 'Không thể xác định địa chỉ từ vị trí hiện tại.', 'error');
      } else {
          showToast('Lỗi vị trí', 'Không thể lấy dữ liệu vị trí vào lúc này.', 'error');
      }
  }

  // Restore button state
  function resetButton(btn, originalText) {
      btn.innerHTML = originalText;
      btn.disabled = false;
  }

  function geolocationErrorName(code) {
      if (code === 1) return 'PERMISSION_DENIED';
      if (code === 2) return 'POSITION_UNAVAILABLE';
      if (code === 3) return 'TIMEOUT';
      return 'UNKNOWN';
  }

  async function inspectGeolocationPermission(btn) {
      let permissionState = 'UNSUPPORTED';
      try {
          if (navigator.permissions && typeof navigator.permissions.query === 'function') {
              const permissionStatus = await navigator.permissions.query({ name: 'geolocation' });
              permissionState = String(permissionStatus.state || 'UNKNOWN').toUpperCase();
          }
      } catch (error) {
          permissionState = 'QUERY_FAILED';
          console.warn('[Geolocation] Permissions API query failed:', error.message);
      }

      btn.dataset.geolocationPermissionState = permissionState;
      console.info('[Geolocation] permission.state =', permissionState);
      return permissionState;
  }

  // Geolocation main controller (Requirement 2, 4, 5, 6, 9)
  async function getCurrentLocation() {
      const btn = document.getElementById('btn-get-location');
      if (!btn || btn.disabled) return; // Prevent double trigger

      const originalText = btn.innerHTML;
      delete btn.dataset.geolocationErrorCode;
      delete btn.dataset.geolocationErrorName;
      delete btn.dataset.geolocationErrorMessage;
      delete btn.dataset.geolocationLatitude;
      delete btn.dataset.geolocationLongitude;
      btn.dataset.geolocationStage = 'PERMISSION_CHECK';
      await inspectGeolocationPermission(btn);

      // 1. Protect user entered data (Requirement 6)
      const streetInput = document.getElementById('address-street');
      const hasUserData = (streetInput && streetInput.value.trim() !== '') || 
                          Boolean(document.getElementById('ghnProvinceId')?.value);

      if (hasUserData) {
          const confirmOverwrite = await SmashNotify.confirm({
              title: 'Thay thế địa chỉ hiện tại?',
              message: 'Địa chỉ hiện tại sẽ bị thay thế bởi vị trí mới. Bạn có muốn tiếp tục không?'
          });
          if (!confirmOverwrite) {
              return; // Cancel
          }
      }

      // The old cache stored failed/partial responses and made later clicks
      // replay stale data instead of requesting the current browser position.
      try {
          localStorage.removeItem('location_geocode_cache_v2');
      } catch (cacheErr) {
          console.warn("[Geocoding] Unable to clear legacy location cache:", cacheErr);
      }

      // 2. Set loading state and request protection (Requirement 5)
      btn.innerHTML = '<i class="fas fa-spinner fa-spin u-s-m-r-8"></i> Đang lấy vị trí hiện tại...';
      btn.disabled = true;
      btn.dataset.geolocationStage = 'REQUESTED';

      // 3. Try browser GPS location first
      if (navigator.geolocation) {
          navigator.geolocation.getCurrentPosition(
              async function(position) {
                  try {
                      const lat = position.coords.latitude;
                      const lon = position.coords.longitude;
                      const accuracy = position.coords.accuracy;
                      btn.dataset.geolocationStage = 'SUCCESS';
                      btn.dataset.geolocationLatitude = String(lat);
                      btn.dataset.geolocationLongitude = String(lon);
                      console.info('[Geolocation] getCurrentPosition succeeded:', {
                          latitude: lat,
                          longitude: lon,
                          accuracy: accuracy
                      });

                      // Validate coordinates (Requirement 5)
                      if (!isValidCoordinates(lat, lon)) {
                          throw new Error("INVALID_COORDINATES");
                      }

                      // Accuracy check (Requirement 2)
                      if (accuracy > 500) {
                          showToast('Độ chính xác thấp', 'Vị trí hiện tại có độ chính xác thấp. Kết quả địa chỉ có thể không hoàn toàn chính xác.', 'warning', 7000);
                      }

                      // Update map marker
                      updateMapFromLocation(lat, lon);

                      // Call reverse geocoding abstraction
                      const parsedAddress = await reverseGeocode(lat, lon);

                      populateAddressFields(parsedAddress);
                      showAddressResolutionResult(parsedAddress, 'Thành công');

                  } catch (err) {
                      console.error('[Geocoding] GPS success branch error:', err);
                      handleGeocodingError(err);
                  } finally {
                      resetButton(btn, originalText);
                  }
              },
              function(error) {
                   const errorCode = Number(error && error.code);
                   const errorName = geolocationErrorName(errorCode);
                   const errorMessage = error && error.message ? String(error.message) : '';
                   btn.dataset.geolocationStage = 'ERROR';
                   btn.dataset.geolocationErrorCode = String(errorCode);
                   btn.dataset.geolocationErrorName = errorName;
                   btn.dataset.geolocationErrorMessage = errorMessage;
                   console.warn('[Geolocation] getCurrentPosition failed:', {
                       code: errorCode,
                       name: errorName,
                       message: errorMessage,
                       permissionState: btn.dataset.geolocationPermissionState || 'UNKNOWN'
                   });
                   showToast('Không thể lấy vị trí',
                           'Không thể truy cập vị trí hiện tại. Vui lòng chọn địa chỉ bên dưới.',
                           'warning', 6000);
                   resetButton(btn, originalText);
              },
              {
                  enableHighAccuracy: true,
                  timeout: 8000, // Timeout after 8 seconds (Requirement 9)
                  maximumAge: 0
              }
          );
      } else {
           console.warn('[Geocoding] Browser does not support Geolocation.');
           showToast('Không hỗ trợ định vị',
                   'Trình duyệt không hỗ trợ lấy vị trí. Vui lòng chọn địa chỉ bên dưới.', 'warning');
           resetButton(btn, originalText);
      }
  }

  // Interactive Leaflet Map initialization and synchronization
  async function initLocationMap(mapDiv) {
      const latInput = document.getElementById('address-lat');
      const lonInput = document.getElementById('address-lon');
      const streetInput = document.getElementById('address-street');

      let initialLat = 21.0285;
      let initialLon = 105.8542;
      let zoomLevel = 13;
      let hasCoordinates = false;

      // 5. Use saved coordinates directly (Requirement 5: Dùng tọa độ đã lưu)
      if (latInput && lonInput && latInput.value && lonInput.value) {
          const lat = parseFloat(latInput.value);
          const lon = parseFloat(lonInput.value);
          if (isValidCoordinates(lat, lon)) {
              initialLat = lat;
              initialLon = lon;
              zoomLevel = 16;
              hasCoordinates = true;
          }
      }

      // Initialize Map
      leafletMap = L.map(mapDiv).setView([initialLat, initialLon], zoomLevel);

      // Tile Layer setup (OpenStreetMap Tiles)
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
          maxZoom: 19,
          attribution: '&copy; <a href="https://openstreetmap.org/copyright">OpenStreetMap</a> contributors'
      }).addTo(leafletMap);

      // Draggable Marker
      mapMarker = L.marker([initialLat, initialLon], {
          draggable: true
      });

      if (hasCoordinates) {
          mapMarker.addTo(leafletMap);
      }

      // Add Custom "Locate Me" Control on the Map (Locate self on the map)
      L.Control.LocateMe = L.Control.extend({
          options: {
              position: 'topleft'
          },
          onAdd: function(map) {
              const container = L.DomUtil.create('div', 'leaflet-bar leaflet-control leaflet-control-custom');
              container.style.backgroundColor = '#fff';
              container.style.width = '34px';
              container.style.height = '34px';
              container.style.lineHeight = '30px';
              container.style.textAlign = 'center';
              container.style.cursor = 'pointer';
              container.style.display = 'flex';
              container.style.alignItems = 'center';
              container.style.justifyContent = 'center';
              container.title = "Định vị vị trí của tôi";

              // Target / Crosshairs icon using FontAwesome
              container.innerHTML = '<a href="#" style="display: flex; align-items: center; justify-content: center; width: 100%; height: 100%; color: #333; font-size: 16px; text-decoration: none;"><i class="fas fa-crosshairs"></i></a>';

              L.DomEvent.disableClickPropagation(container);
              L.DomEvent.on(container, 'click', function(e) {
                  L.DomEvent.preventDefault(e);
                  getCurrentLocation();
              });

              return container;
          }
      });
      leafletMap.addControl(new L.Control.LocateMe());

      // Click event on map to select position
      leafletMap.on('click', function(e) {
          const { lat, lng } = e.latlng;
          if (!isValidCoordinates(lat, lng)) return;

          mapMarker.setLatLng(e.latlng);
          if (!leafletMap.hasLayer(mapMarker)) {
              mapMarker.addTo(leafletMap);
          }

          if (latInput) latInput.value = lat.toFixed(6);
          if (lonInput) lonInput.value = lng.toFixed(6);

          debouncedReverseGeocode(lat, lng);
      });

      // Marker drag event (de-bounced geocode lookup)
      mapMarker.on('dragend', function(e) {
          const latlng = mapMarker.getLatLng();
          const { lat, lng } = latlng;
          if (!isValidCoordinates(lat, lng)) return;

          if (latInput) latInput.value = lat.toFixed(6);
          if (lonInput) lonInput.value = lng.toFixed(6);

          debouncedReverseGeocode(lat, lng);
      });

      // If Edit form has no saved coordinates, center it using the canonical
      // option labels already loaded from the backend delivery-area catalog.
      if (!hasCoordinates && streetInput && streetInput.value && window.SmashAddressBook?.ready) {
          try {
              await window.SmashAddressBook.ready();
              const selectedText = function (selectId) {
                  const select = document.getElementById(selectId);
                  const option = select?.options[select.selectedIndex];
                  return option && option.value ? option.textContent.trim() : '';
              };
              const addressQuery = [streetInput.value,
                  selectedText('ghnWardCode'),
                  selectedText('ghnDistrictId'),
                  selectedText('ghnProvinceId'),
                  'Việt Nam'].filter(Boolean).join(', ');
              if (!addressQuery) return;
              const response = await fetchWithTimeout(`/api/location/search?q=${encodeURIComponent(addressQuery)}`, { timeout: 5000 });
              if (response.ok) {
                  const data = await response.json();
                  const lat = data.latitude;
                  const lon = data.longitude;
                  if (isValidCoordinates(lat, lon)) {
                      leafletMap.setView([lat, lon], 16);
                      mapMarker.setLatLng([lat, lon]).addTo(leafletMap);
                      if (latInput) latInput.value = lat.toFixed(6);
                      if (lonInput) lonInput.value = lon.toFixed(6);
                  }
              }
          } catch (err) {
              console.warn("[Map] Silent forward geocoding failed:", err.message);
          }
      }

      // If Add form and NO coordinates are saved, center map using silent IP lookup (no UI alerts)
      if (!hasCoordinates && (!streetInput || !streetInput.value)) {
          try {
              const response = await fetchWithTimeout('/api/location/ip', { timeout: 4000 });
              if (response.ok) {
                  const data = await response.json();
                  const lat = data.latitude;
                  const lon = data.longitude;
                  if (isValidCoordinates(lat, lon)) {
                      leafletMap.setView([lat, lon], 14);
                  }
              }
          } catch (err) {
              console.warn("[Map] Silent IP lookup failed:", err.message);
          }
      }
  }

  // DOM initialization wrapper for Map
  $(document).ready(function() {
      const mapDiv = document.getElementById('map');
      if (mapDiv) {
          initLocationMap(mapDiv);
      }
      const locationButton = document.getElementById('btn-get-location');
      if (locationButton) {
          locationButton.addEventListener('click', getCurrentLocation);
      }
  });
  /*==============================================================
    # CUSTOM JS: Xóa Sổ Địa Chỉ bằng AJAX
  ==============================================================*/
  $(document).on('click', '.js-delete-address', function(e) {
      e.preventDefault();
      var btn = $(this);
      var idDiaChi = btn.data('id');

      SmashNotify.confirm({
          title: 'Xóa địa chỉ?',
          message: 'Bạn có chắc chắn muốn xóa địa chỉ này?',
          confirmText: 'Xóa',
          danger: true
      }).then(function(confirmed) {
          if (!confirmed) return;
          // Làm mờ dòng đó đi để báo hiệu đang chờ máy chủ
          var tr = btn.closest('tr');
          tr.css('opacity', '0.5');

          $.ajax({
              url: '/user/address/api/delete/' + idDiaChi,
              type: 'GET',
              success: function(response) {
                  if (response.trangThai === 'chuadangnhap') {
                      window.location.href = '/user/dang-nhap';
                      return;
                  }
                  
                  if (response.trangThai === 'ok') {
                      // Mờ dần và xóa dòng HTML
                      tr.fadeOut(300, function() {
                          $(this).remove();
                          
                          // Nếu xóa hết sạch, hiển thị câu thông báo trống
                          if ($('table.dash__table-2 tbody tr').length === 0) {
                              var emptyMsg = '<tr><td colspan="6" class="text-center text-muted py-4">Bạn chưa có địa chỉ giao hàng nào. Hãy thêm mới nhé!</td></tr>';
                              $('table.dash__table-2 tbody').html(emptyMsg);
                          }
                      });
                  } else {
                      showToast(response.message, "error");
                      tr.css('opacity', '1'); // Khôi phục lại hiển thị
                  }
              },
              error: function() {
                  showToast("Lỗi kết nối máy chủ!", "error");
                  tr.css('opacity', '1');
              }
          });
      });
  });

/* ==============================================================
   # SMASH CART PAGE MANAGEMENT & SUMMARY LOGIC
   ============================================================== */

const cartQuantityStates = new Map();

function getQuantityState(row) {
    const itemId = row.getAttribute('data-item-id');
    if (!cartQuantityStates.has(itemId)) {
        const initialQty = parseInt(row.getAttribute('data-quantity')) || 1;
        cartQuantityStates.set(itemId, {
            desiredQuantity: initialQty,
            confirmedQuantity: initialQty,
            requestVersion: 0,
            debounceTimer: null,
            pendingPromise: null
        });
    }
    return cartQuantityStates.get(itemId);
}

function formatVnd(amount) {
    return new Intl.NumberFormat('vi-VN').format(amount || 0) + ' đ';
}

function recalculateSelectedCartSummary() {
    const rows = Array.from(document.querySelectorAll('.js-cart-row'));
    const validRows = rows.filter(row => {
        const val = row.getAttribute('data-valid') || row.dataset.valid;
        return val === 'true' || val === true;
    });
    const totalValidCount = validRows.length;

    let selectedItemCount = 0;
    let selectedSubtotal = 0;

    validRows.forEach(row => {
        const checkbox = row.querySelector('.js-cart-item-checkbox');
        if (checkbox && checkbox.checked) {
            selectedItemCount++;
            const lineTotalAttr = row.getAttribute('data-line-total') || row.dataset.lineTotal;
            const lineTotal = parseFloat(lineTotalAttr) || 0;
            selectedSubtotal += lineTotal;
        }
    });

    const formattedSubtotal = formatVnd(selectedSubtotal);

    const subtotalEl = document.querySelector('.js-cart-summary-subtotal');
    if (subtotalEl) subtotalEl.textContent = formattedSubtotal;

    const totalEl = document.querySelector('.js-cart-summary-total');
    if (totalEl) totalEl.textContent = formattedSubtotal;

    const selectedCountEl = document.querySelector('#js-selected-count') || document.getElementById('js-selected-count');
    if (selectedCountEl) selectedCountEl.textContent = selectedItemCount;

    const summarySelectedCountEl = document.querySelector('#js-summary-selected-count') || document.getElementById('js-summary-selected-count');
    if (summarySelectedCountEl) summarySelectedCountEl.textContent = selectedItemCount;

    const totalValidCountEl = document.querySelector('#js-total-valid-count') || document.getElementById('js-total-valid-count');
    if (totalValidCountEl) totalValidCountEl.textContent = totalValidCount;

    const selectAllEl = document.querySelector('#js-cart-select-all') || document.querySelector('.js-cart-select-all');
    if (selectAllEl) {
        if (totalValidCount === 0) {
            selectAllEl.checked = false;
            selectAllEl.indeterminate = false;
            selectAllEl.disabled = true;
        } else if (selectedItemCount === 0) {
            selectAllEl.checked = false;
            selectAllEl.indeterminate = false;
            selectAllEl.disabled = false;
        } else if (selectedItemCount === totalValidCount) {
            selectAllEl.checked = true;
            selectAllEl.indeterminate = false;
            selectAllEl.disabled = false;
        } else {
            selectAllEl.checked = false;
            selectAllEl.indeterminate = true;
            selectAllEl.disabled = false;
        }
    }

    const checkoutBtn = document.querySelector('#js-start-checkout-btn') || document.getElementById('js-start-checkout-btn');
    if (checkoutBtn) {
        if (selectedItemCount > 0) {
            checkoutBtn.disabled = false;
            checkoutBtn.classList.remove('btn-disabled', 'opacity-50');
            checkoutBtn.style.cursor = 'pointer';
            checkoutBtn.style.pointerEvents = 'auto';
        } else {
            checkoutBtn.disabled = true;
            checkoutBtn.classList.add('btn-disabled', 'opacity-50');
            checkoutBtn.style.cursor = 'not-allowed';
        }
    }

    const deleteSelectedBtn = document.querySelector('#js-delete-selected-cart-items') || document.querySelector('.js-delete-selected-cart-items');
    if (deleteSelectedBtn) {
        deleteSelectedBtn.disabled = selectedItemCount === 0;
    }
}

window.recalculateSelectedCartSummary = recalculateSelectedCartSummary;

function getFriendlyErrorMessage(error, defaultMsg) {
    const fallback = defaultMsg || 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại kết nối Internet và thử lại.';
    if (!error) return fallback;
    let msg = '';
    if (typeof error === 'string') {
        msg = error;
    } else if (error.responseJSON && (error.responseJSON.message || error.responseJSON.thongBao)) {
        msg = error.responseJSON.message || error.responseJSON.thongBao;
    } else if (error.message) {
        msg = error.message;
    } else if (error.responseText && typeof error.responseText === 'string' && error.responseText.length < 300) {
        msg = error.responseText;
    }
    if (!msg || typeof msg !== 'string') return fallback;

    const technicalPatterns = [
        'failed to fetch', 'networkerror', 'load failed', 'internal server error',
        'maximum upload size exceeded', 'aborterror', 'typeerror: failed to fetch',
        'unexpected end of json input', 'syntaxerror', 'cannot read properties',
        'something went wrong', 'unexpected error', 'request failed'
    ];
    const lower = msg.toLowerCase();
    for (let i = 0; i < technicalPatterns.length; i++) {
        if (lower.includes(technicalPatterns[i])) {
            return fallback;
        }
    }
    return msg;
}
window.getFriendlyErrorMessage = getFriendlyErrorMessage;

function updateQuantityUiImmediately(row, newQuantity) {
    if (!row || !document.body.contains(row)) return;
    const input = row.querySelector('.js-cart-qty-input');
    if (input) {
        input.value = newQuantity;
    }

    const unitPrice = parseFloat(row.getAttribute('data-unit-price')) || 0;
    const tempLineTotal = unitPrice * newQuantity;

    row.setAttribute('data-quantity', newQuantity);
    row.setAttribute('data-line-total', tempLineTotal);
    row.dataset.quantity = String(newQuantity);
    row.dataset.lineTotal = String(tempLineTotal);

    const lineTotalEl = row.querySelector('.js-cart-line-total');
    if (lineTotalEl) {
        lineTotalEl.textContent = formatVnd(tempLineTotal);
    }

    recalculateSelectedCartSummary();
}

function persistQuantity(row, quantityToSave, state) {
    if (!row || !document.body.contains(row) || (state && state.deleted)) {
        return Promise.resolve({ trangThai: 'ignored' });
    }
    const itemId = row.getAttribute('data-item-id');
    const version = ++state.requestVersion;

    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    return new Promise((resolve, reject) => {
        $.ajax({
            url: '/gio-hang/cap-nhat',
            type: 'POST',
            data: { idChiTiet: parseInt(itemId), soLuong: quantityToSave },
            beforeSend: function(jqXHR) {
                if (state) state.activeXhr = jqXHR;
                if (token && header) jqXHR.setRequestHeader(header, token);
            },
            success: function(res) {
                if (state) state.activeXhr = null;
                // Latest-write-wins & deleted check & DOM check
                if (version !== state.requestVersion || (state && state.deleted) || !document.body.contains(row)) {
                    resolve(res);
                    return;
                }

                if (res && res.trangThai === 'ok') {
                    const serverQty = res.quantity != null ? res.quantity : quantityToSave;
                    const unitPrice = res.unitPrice != null ? res.unitPrice : (parseFloat(row.getAttribute('data-unit-price')) || 0);
                    const lineTotal = res.lineTotal != null ? res.lineTotal : (unitPrice * serverQty);

                    state.confirmedQuantity = serverQty;
                    state.desiredQuantity = serverQty;

                    const input = row.querySelector('.js-cart-qty-input');
                    if (input) input.value = serverQty;

                    row.setAttribute('data-quantity', serverQty);
                    row.setAttribute('data-unit-price', unitPrice);
                    row.setAttribute('data-line-total', lineTotal);
                    row.dataset.quantity = String(serverQty);
                    row.dataset.unitPrice = String(unitPrice);
                    row.dataset.lineTotal = String(lineTotal);

                    const lineTotalEl = row.querySelector('.js-cart-line-total');
                    if (lineTotalEl) lineTotalEl.textContent = formatVnd(lineTotal);

                    recalculateSelectedCartSummary();
                    resolve(res);
                } else {
                    if ((state && state.deleted) || !document.body.contains(row)) {
                        resolve(res);
                        return;
                    }
                    // Rollback to confirmedQuantity
                    const rollbackQty = state.confirmedQuantity;
                    state.desiredQuantity = rollbackQty;

                    updateQuantityUiImmediately(row, rollbackQty);

                    const rawMsg = (res && res.message) || 'Không thể cập nhật số lượng.';
                    const msg = getFriendlyErrorMessage(rawMsg, 'Không thể cập nhật số lượng.');
                    showToast('Cập nhật thất bại: ' + msg, 'error');
                    reject(new Error(msg));
                }
            },
            error: function(err) {
                if (state) state.activeXhr = null;
                if (version !== state.requestVersion || (state && state.deleted) || !document.body.contains(row)) {
                    resolve({ trangThai: 'ignored' });
                    return;
                }

                const rollbackQty = state.confirmedQuantity;
                state.desiredQuantity = rollbackQty;

                updateQuantityUiImmediately(row, rollbackQty);

                const rawMsg = (err.responseJSON && err.responseJSON.message) || err.responseText || 'Đã xảy ra lỗi khi cập nhật số lượng.';
                const msg = getFriendlyErrorMessage(rawMsg, 'Không thể kết nối đến máy chủ. Vui lòng thử lại sau.');
                showToast(msg, 'error');
                reject(err);
            }
        });
    });
}

function scheduleQuantityUpdate(row, quantity) {
    const state = getQuantityState(row);

    if (state.debounceTimer) {
        clearTimeout(state.debounceTimer);
        state.debounceTimer = null;
    }

    state.debounceTimer = setTimeout(() => {
        state.debounceTimer = null;
        state.pendingPromise = persistQuantity(row, quantity, state);
    }, 800);
}

function changeDesiredQuantity(row, delta) {
    const state = getQuantityState(row);
    const min = 1;
    const input = row.querySelector('.js-cart-qty-input');
    const max = Number(row.getAttribute('data-max-quantity') || (input ? input.getAttribute('data-max') : 999) || 999);

    const nextQuantity = Math.min(max, Math.max(min, state.desiredQuantity + delta));

    if (nextQuantity === state.desiredQuantity) {
        return;
    }

    state.desiredQuantity = nextQuantity;
    updateQuantityUiImmediately(row, nextQuantity);
    scheduleQuantityUpdate(row, nextQuantity);
}

function setDesiredQuantity(row, targetQuantity) {
    const state = getQuantityState(row);
    const min = 1;
    const input = row.querySelector('.js-cart-qty-input');
    const max = Number(row.getAttribute('data-max-quantity') || (input ? input.getAttribute('data-max') : 999) || 999);

    let nextQuantity = parseInt(targetQuantity);
    if (isNaN(nextQuantity) || nextQuantity < min) nextQuantity = min;
    if (nextQuantity > max) nextQuantity = max;

    if (nextQuantity === state.desiredQuantity) {
        updateQuantityUiImmediately(row, nextQuantity);
        return;
    }

    state.desiredQuantity = nextQuantity;
    updateQuantityUiImmediately(row, nextQuantity);
    scheduleQuantityUpdate(row, nextQuantity);
}

async function flushAllQuantityUpdates() {
    const promises = [];

    for (const [itemId, state] of cartQuantityStates) {
        const row = document.querySelector(`.js-cart-row[data-item-id="${itemId}"]`);
        if (state.debounceTimer) {
            clearTimeout(state.debounceTimer);
            state.debounceTimer = null;

            if (row) {
                const promise = persistQuantity(row, state.desiredQuantity, state);
                state.pendingPromise = promise;
                promises.push(promise);
            }
        } else if (state.pendingPromise) {
            promises.push(state.pendingPromise);
        }
    }

    if (promises.length > 0) {
        await Promise.all(promises);
    }
}

function initializeCartPage() {
    if (window.__smashCartInitialized) {
        return;
    }

    const selectAllBtn = document.querySelector('#js-cart-select-all') || document.querySelector('.js-cart-select-all');
    const checkoutBtn = document.querySelector('#js-start-checkout-btn') || document.getElementById('js-start-checkout-btn');
    const hasCartRows = document.querySelectorAll('.js-cart-row').length > 0;

    if (!selectAllBtn && !checkoutBtn && !hasCartRows) {
        return;
    }

    window.__smashCartInitialized = true;

    // Disable template RESHOP.initInputCounter for cart items to prevent duplicate handlers
    $('.js-cart-row .input-counter').find('.input-counter__plus, .input-counter__minus').off('click');
    $('.js-cart-row .input-counter').find('input').off('change');

    if (selectAllBtn) {
        selectAllBtn.addEventListener('change', function() {
            const checked = this.checked;
            const validRows = Array.from(document.querySelectorAll('.js-cart-row')).filter(r => (r.getAttribute('data-valid') || r.dataset.valid) === 'true');
            validRows.forEach(row => {
                const cb = row.querySelector('.js-cart-item-checkbox');
                if (cb && !cb.disabled) {
                    cb.checked = checked;
                }
            });
            recalculateSelectedCartSummary();
        });
    }

    document.addEventListener('change', function(e) {
        if (e.target && e.target.classList.contains('js-cart-item-checkbox')) {
            recalculateSelectedCartSummary();
        }
    });

    // Event delegation for Quantity Plus / Minus buttons
    document.addEventListener('click', function(e) {
        const plusBtn = e.target.closest('.js-cart-qty-plus') || (e.target.classList.contains('input-counter__plus') ? e.target : null);
        const minusBtn = e.target.closest('.js-cart-qty-minus') || (e.target.classList.contains('input-counter__minus') ? e.target : null);

        if (plusBtn) {
            e.preventDefault();
            e.stopPropagation();
            const row = plusBtn.closest('.js-cart-row');
            if (row) changeDesiredQuantity(row, 1);
        } else if (minusBtn) {
            e.preventDefault();
            e.stopPropagation();
            const row = minusBtn.closest('.js-cart-row');
            if (row) changeDesiredQuantity(row, -1);
        }
    });

    // Direct input typing change
    document.addEventListener('change', function(e) {
        if (e.target && e.target.classList.contains('js-cart-qty-input')) {
            const row = e.target.closest('.js-cart-row');
            if (row) setDesiredQuantity(row, e.target.value);
        }
    });

    if (checkoutBtn) {
        checkoutBtn.addEventListener('click', async function(e) {
            e.preventDefault();

            checkoutBtn.disabled = true;
            checkoutBtn.classList.add('btn-disabled', 'opacity-50');
            const originalText = checkoutBtn.textContent;
            checkoutBtn.textContent = 'ĐANG CẬP NHẬT...';

            try {
                await flushAllQuantityUpdates();
            } catch (err) {
                showToast('Vui lòng kiểm tra lại số lượng sản phẩm.', 'error');
                checkoutBtn.textContent = originalText;
                recalculateSelectedCartSummary();
                return;
            }

            const validSelectedRows = Array.from(document.querySelectorAll('.js-cart-row'))
                .filter(row => (row.getAttribute('data-valid') || row.dataset.valid) === 'true');

            const selectedIds = [];
            validSelectedRows.forEach(row => {
                const cb = row.querySelector('.js-cart-item-checkbox');
                if (cb && cb.checked && cb.value) {
                    selectedIds.push(parseInt(cb.value));
                }
            });

            if (selectedIds.length === 0) {
                showToast('Vui lòng chọn ít nhất một sản phẩm để thanh toán.', 'warning');
                checkoutBtn.textContent = originalText;
                recalculateSelectedCartSummary();
                return;
            }

            checkoutBtn.textContent = 'ĐANG CHUYỂN HƯỚNG...';

            const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

            $.ajax({
                url: '/checkout/start',
                type: 'POST',
                data: { selectedItemIds: selectedIds.join(',') },
                beforeSend: function(xhr) {
                    if (token && header) xhr.setRequestHeader(header, token);
                },
                success: function(res) {
                    if (res && res.trangThai === 'ok' && res.checkoutUrl) {
                        window.location.href = res.checkoutUrl;
                    } else {
                        const rawMsg = (res && res.message) || 'Không thể khởi tạo thanh toán.';
                        const msg = getFriendlyErrorMessage(rawMsg, 'Không thể khởi tạo thanh toán. Vui lòng thử lại sau.');
                        showToast(msg, 'error');
                        checkoutBtn.textContent = originalText;
                        recalculateSelectedCartSummary();
                    }
                },
                error: function(err) {
                    const rawMsg = (err.responseJSON && err.responseJSON.message) || err.responseText || 'Có lỗi xảy ra, vui lòng thử lại.';
                    const msg = getFriendlyErrorMessage(rawMsg, 'Không thể kết nối đến máy chủ. Vui lòng thử lại sau.');
                    showToast(msg, 'error');
                    checkoutBtn.textContent = originalText;
                    recalculateSelectedCartSummary();
                }
            });
        });
    }

    const deleteSelectedBtn = document.querySelector('#js-delete-selected-cart-items') || document.querySelector('.js-delete-selected-cart-items');
    if (deleteSelectedBtn) {
        deleteSelectedBtn.addEventListener('click', function(e) {
            e.preventDefault();
            if (window.__bulkCartDeleteInFlight) return;

            const validSelectedRows = Array.from(document.querySelectorAll('.js-cart-row'))
                .filter(row => (row.getAttribute('data-valid') || row.dataset.valid) === 'true');

            const selectedIds = [];
            const selectedRows = [];
            validSelectedRows.forEach(row => {
                const cb = row.querySelector('.js-cart-item-checkbox');
                if (cb && cb.checked && cb.value) {
                    selectedIds.push(parseInt(cb.value));
                    selectedRows.push(row);
                }
            });

            if (selectedIds.length === 0) return;

            const lineCount = selectedIds.length;
            const confirmText = `${lineCount} sản phẩm sẽ được xóa khỏi giỏ hàng.`;

            SmashNotify.confirm({
                title: 'Xóa sản phẩm đã chọn?',
                message: confirmText,
                confirmText: 'Xóa',
                danger: true
            }).then((confirmed) => {
                if (confirmed) {
                    executeBulkDelete(selectedIds, selectedRows);
                }
            });
        });
    }

    recalculateSelectedCartSummary();
}

window.__bulkCartDeleteInFlight = false;

function executeBulkDelete(selectedIds, selectedRows) {
    if (window.__bulkCartDeleteInFlight) return;
    window.__bulkCartDeleteInFlight = true;

    const deleteBtn = document.querySelector('#js-delete-selected-cart-items') || document.querySelector('.js-delete-selected-cart-items');
    const checkoutBtn = document.querySelector('#js-start-checkout-btn') || document.getElementById('js-start-checkout-btn');
    const selectAllBtn = document.querySelector('#js-cart-select-all') || document.querySelector('.js-cart-select-all');

    if (deleteBtn) deleteBtn.disabled = true;
    if (checkoutBtn) checkoutBtn.disabled = true;
    if (selectAllBtn) selectAllBtn.disabled = true;

    selectedRows.forEach(row => {
        const itemId = row.getAttribute('data-item-id');
        const state = typeof cartQuantityStates !== 'undefined' ? (cartQuantityStates.get(itemId) || cartQuantityStates.get(String(itemId)) || cartQuantityStates.get(Number(itemId))) : null;
        if (state) {
            state.deleted = true;
            state.requestVersion = (state.requestVersion || 0) + 1;
            if (state.debounceTimer) {
                clearTimeout(state.debounceTimer);
                state.debounceTimer = null;
            }
            if (state.activeXhr && typeof state.activeXhr.abort === 'function') {
                try { state.activeXhr.abort(); } catch (e) {}
                state.activeXhr = null;
            }
        }
    });

    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    $.ajax({
        url: '/gio-hang/api/xoa-nhieu',
        type: 'POST',
        traditional: true,
        data: { selectedItemIds: selectedIds },
        beforeSend: function(xhr) {
            if (token && header) xhr.setRequestHeader(header, token);
        },
        success: function(res) {
            if (res && res.trangThai === 'ok') {
                let removedCount = 0;
                selectedRows.forEach(row => {
                    const itemId = row.getAttribute('data-item-id');
                    if (typeof cartQuantityStates !== 'undefined') {
                        cartQuantityStates.delete(itemId);
                        cartQuantityStates.delete(String(itemId));
                        cartQuantityStates.delete(Number(itemId));
                    }
                    $(row).fadeOut(300, function() {
                        $(this).remove();
                        removedCount++;
                        if (removedCount === selectedRows.length) {
                            handlePostDeleteSync();
                        }
                    });
                });
                if (selectedRows.length === 0) {
                    handlePostDeleteSync();
                }
            } else {
                const rawMsg = (res && (res.thongBao || res.message)) || 'Không thể xóa các sản phẩm đã chọn.';
                const msg = getFriendlyErrorMessage(rawMsg, 'Không thể xóa các sản phẩm đã chọn.');
                showToast(msg, 'error');
            }
        },
        error: function(err) {
            const rawMsg = (err.responseJSON && (err.responseJSON.thongBao || err.responseJSON.message)) || 'Đã xảy ra lỗi khi xóa sản phẩm.';
            const msg = getFriendlyErrorMessage(rawMsg, 'Không thể kết nối đến máy chủ. Vui lòng thử lại sau.');
            showToast(msg, 'error');
        },
        complete: function() {
            window.__bulkCartDeleteInFlight = false;
            recalculateSelectedCartSummary();
        }
    });
}

function handlePostDeleteSync() {
    const remainingRows = document.querySelectorAll('.js-cart-row').length;
    if (remainingRows === 0) {
        const tbody = document.querySelector('table.table-p tbody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center u-s-p-y-30">Giỏ hàng của bạn đang trống. Hãy thêm sản phẩm vào giỏ nhé!</td></tr>';
        }
        $('.f-cart').fadeOut();
        $('.cart-action-bar').fadeOut();
    }
    recalculateSelectedCartSummary();
    if (typeof loadMiniCart === 'function') {
        loadMiniCart();
    }
}

window.flushAllCartQuantityUpdates = flushAllQuantityUpdates;
window.__miniCartCheckoutInFlight = false;

function getCsrfHeaders() {
    let tokenElement = document.querySelector('meta[name="_csrf"]');
    let headerElement = document.querySelector('meta[name="_csrf_header"]');

    let token = tokenElement ? tokenElement.content : null;
    let headerName = headerElement ? headerElement.content : null;

    if (!token || !headerName) {
        const holder = document.getElementById('csrf-token-holder');
        if (holder) {
            token = holder.getAttribute('data-csrf-token') || holder.dataset.csrfToken;
            headerName = holder.getAttribute('data-csrf-header') || holder.dataset.csrfHeader;
        }
    }

    const headers = {};
    if (token && headerName && token.trim().length > 0 && headerName.trim().length > 0) {
        headers[headerName] = token;
    }
    return headers;
}

function setMiniCartCheckoutLoading(loading) {
    document.querySelectorAll('.js-mini-cart-checkout-btn').forEach(button => {
        if (!button.dataset.originalText) {
            button.dataset.originalText = button.textContent.trim();
        }
        button.disabled = loading;
        button.textContent = loading ? 'ĐANG XỬ LÝ...' : button.dataset.originalText;
    });
}

async function startAllCartCheckout(button) {
    if (window.__miniCartCheckoutInFlight) {
        return;
    }

    window.__miniCartCheckoutInFlight = true;
    setMiniCartCheckoutLoading(true);

    try {
        if (typeof window.flushAllCartQuantityUpdates === 'function') {
            await window.flushAllCartQuantityUpdates();
        }
    } catch (flushError) {
        console.error("Flush quantity updates failed:", flushError);
        window.__miniCartCheckoutInFlight = false;
        setMiniCartCheckoutLoading(false);
        showToast('Vui lòng kiểm tra lại số lượng sản phẩm.', 'error');
        return;
    }

    try {
        const csrfHeaders = getCsrfHeaders();
        const response = await fetch('/checkout/start-all', {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                ...csrfHeaders,
                'Accept': 'application/json'
            }
        });

        const rawBody = await response.text();

        let data = {};
        try {
            data = rawBody ? JSON.parse(rawBody) : {};
        } catch (parseError) {
            console.error('start-all returned non-JSON:', response.status, response.headers.get('content-type'), rawBody);
            throw new Error(`Máy chủ trả dữ liệu không hợp lệ (HTTP ${response.status})`);
        }

        if (!response.ok || data.trangThai !== 'ok') {
            const invalidDetails = Array.isArray(data.invalidItems)
                ? data.invalidItems
                    .map(item => `${item.tenSanPham || 'Sản phẩm'}: ${item.reason || item.lyDo || 'Không hợp lệ'}`)
                    .join('\n')
                : '';

            throw new Error(
                invalidDetails ||
                data.thongBao ||
                data.message ||
                `Không thể bắt đầu thanh toán (HTTP ${response.status})`
            );
        }

        if (!data.checkoutUrl) {
            throw new Error('Hệ thống không trả về đường dẫn thanh toán. Vui lòng thử lại sau.');
        }

        window.location.href = data.checkoutUrl;
    } catch (error) {
        console.error("startAllCartCheckout error:", error);
        const userMsg = getFriendlyErrorMessage(error, 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại kết nối Internet và thử lại.');
        showToast('Thanh toán thất bại: ' + userMsg, 'error');
    } finally {
        window.__miniCartCheckoutInFlight = false;
        setMiniCartCheckoutLoading(false);
    }
}

if (!window.__miniCartCheckoutInitialized) {
    window.__miniCartCheckoutInitialized = true;

    document.addEventListener('click', async function (event) {
        const button = event.target.closest('.js-mini-cart-checkout-btn');
        if (!button) return;

        event.preventDefault();
        event.stopPropagation();

        await startAllCartCheckout(button);
    });

    document.addEventListener('click', function (event) {
        const button = event.target.closest('.js-quick-add-checkout-btn');
        if (!button) return;

        event.preventDefault();
        event.stopPropagation();

        const checkoutUrl = button.getAttribute('data-checkout-url') || $(button).data('checkout-url');
        if (checkoutUrl && checkoutUrl.trim().length > 0) {
            if (typeof $ !== 'undefined' && $('#add-to-cart').length) {
                $('#add-to-cart').modal('hide');
            }
            window.location.href = checkoutUrl;
        } else {
            showToast('Chưa có thông tin thanh toán cho sản phẩm này.', 'error');
        }
    });
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeCartPage);
} else {
    initializeCartPage();
}
