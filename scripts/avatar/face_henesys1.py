# Denma the Owner : Plastic Surgeon (1052004)
#   Henesys : Henesys Plastic Surgery (100000103)

VIP_FACE_M = [
    20000, # Motivated Look
    20001, # Perplexed Stare
    20002, # Leisure Look
    20003, # Dramatic Face
    20004, # Rebel's Fire
    20005, # Alert Face
    20006, # Babyface Pout
    20007, # Sad Innocence
    20008, # Worrisome Glare
    20012, # Curious Dog
    20014, # Look of Wonder
]
VIP_FACE_F = [
    21000, # Motivated Look
    21001, # Fearful Stare
    21002, # Leisure Look
    21003, # Strong Stare
    21004, # Angel Glow
    21005, # Babyface Pout
    21006, # Pucker Up Face
    21007, # Dollface Look
    21008, # Hopeless Gaze
    21012, # Soul's Window
    21014, # Curious Look
]

FACE_COUPON_VIP = 5152057

color = (sm.getFace() % 1000) - (sm.getFace() % 100)
choices = [ face + color for face in (VIP_FACE_M if sm.getGender() == 0 else VIP_FACE_F) ]
answer = sm.askAvatar("Let's see... for #b#t5152057##k, you can get a new face. That's right. I can completely transform your face! Wanna give it a shot? Please consider your choice carefully.", choices)
if answer >= 0 and answer < len(choices):
    if sm.removeItem(FACE_COUPON_VIP, 1):
        sm.changeAvatar(choices[answer])
        sm.sayNext("Enjoy your new and improved face!")
    else:
        sm.sayNext("Hmmm...it looks like you don't have our designated coupon...I'm afraid I can't perform plastic surgery for you without it. I'm sorry...")