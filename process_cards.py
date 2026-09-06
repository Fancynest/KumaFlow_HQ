import os
from PIL import Image

def process_cards():
    input_dir = 'aset_untuk_kumaflow/cards'
    output_dir = 'app/src/main/res/drawable'
    logo_path = 'app/src/main/res/drawable/ic_kumaflow_logo.png'
    
    if not os.path.exists(logo_path):
        print('Logo not found!')
        return

    logo = Image.open(logo_path).convert('RGBA')
    
    card_w, card_h = 1050, 600
    
    logo_target_h = 110
    logo_target_w = int(logo.width * (logo_target_h / logo.height))
    logo = logo.resize((logo_target_w, logo_target_h), Image.Resampling.LANCZOS)
    
    # We will use 1.05x scale in Compose.
    # 1.05x crops 2.5% on each side.
    # 1050 * 0.025 = 26px
    # 600 * 0.025 = 15px
    # Let's add that to the 40px base padding.
    pad_x = 26 + 40
    pad_y = 15 + 40
    logo_pos = (card_w - logo_target_w - pad_x, card_h - logo_target_h - pad_y)

    mapping = {
        'bali_card.jpeg': 'bali_card.webp',
        'bugis_card.jpeg': 'bugis_card.webp',
        'java_card.jpeg': 'java_card.webp',
        'kalbar_card.jpeg': 'westkalimantan_card.webp',
        'minangkabau_card.jpeg': 'minangkabau_card.webp',
        'papua_card.jpeg': 'papua_card.webp',
        'bear.png': 'bear.webp',
        'pride.png': 'pride.webp'
    }

    for in_name, out_name in mapping.items():
        in_path = os.path.join(input_dir, in_name)
        if not os.path.exists(in_path):
            continue
            
        card = Image.open(in_path).convert('RGBA')

        if in_name in ('bear.png', 'pride.png'):
            card = card.crop((56, 35, 994, 571))
        else:
            target_ratio = card_w / card_h
            img_ratio = card.width / card.height
            
            if img_ratio > target_ratio:
                new_w = int(target_ratio * card.height)
                left = (card.width - new_w) // 2
                card = card.crop((left, 0, left + new_w, card.height))
            else:
                new_h = int(card.width / target_ratio)
                top = (card.height - new_h) // 2
                card = card.crop((0, top, card.width, top + new_h))
            
        card = card.resize((card_w, card_h), Image.Resampling.LANCZOS)
        card.paste(logo, logo_pos, logo)
        
        out_path = os.path.join(output_dir, out_name)
        card.save(out_path, 'WEBP', quality=100, lossless=True)
        if out_name == 'bear.webp':
            card.save(os.path.join(output_dir, 'bear2.png'), 'PNG')
        print(f'Saved {out_name}')

process_cards()
